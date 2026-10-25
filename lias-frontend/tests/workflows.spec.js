import { test, expect } from "@playwright/test";
import { execFileSync } from "node:child_process";

function databaseState(email) {
  const query = `SELECT json_build_object('compte',u.id,'membre',m.id,'etat',u.etat_compte,'admin',u.est_admin,'passwordAbsent',u.mot_de_passe_hash IS NULL,'tokenPresent',u.token_activation IS NOT NULL,'expirationPresent',u.token_expiration IS NOT NULL,'affiliations',(SELECT count(*) FROM affiliation a WHERE a.membre_id=m.id AND a.date_fin IS NULL),'statut',(SELECT type_statut FROM historique_statut s WHERE s.membre_id=m.id AND s.date_fin IS NULL),'demande',d.statut_demande,'membreLie',d.membre_cree_id=m.id,'decisionPresente',d.date_decision IS NOT NULL AND d.traite_par_membre_id IS NOT NULL) FROM utilisateur u JOIN membre m ON m.utilisateur_id=u.id JOIN demande_adhesion d ON d.membre_cree_id=m.id WHERE u.email='${email}';`;
  return JSON.parse(
    execFileSync(
      "psql",
      ["-h", "localhost", "-U", "lias", "-d", "lias", "-t", "-A", "-c", query],
      {
        encoding: "utf8",
        env: { ...process.env, PGPASSWORD: process.env.LIAS_DB_PASSWORD },
      },
    ).trim(),
  );
}

async function login(page, role) {
  await page.goto("/connexion");
  await page.getByLabel("Adresse e-mail").fill(role + "@lias.ma");
  await page.getByLabel("Mot de passe").fill("LiasDemo2026!");
  await page.getByRole("button", { name: "Se connecter", exact: true }).click();
  await expect(page.getByRole("heading", { name: /Bonjour/ })).toBeVisible();
}

test("candidature avec CV, acceptation, invitation locale et activation réelle", async ({
  page,
  request,
  context,
}) => {
  const email = `candidat.demo.${Date.now()}@example.ma`;
  const loginResponse = await request.post("/api/auth/login", {
    data: { email: "directeur@lias.ma", motDePasse: "LiasDemo2026!" },
  });
  const { accessToken } = await loginResponse.json();
  const file = await request.get("/api/documents/1/telecharger", {
    headers: { Authorization: "Bearer " + accessToken },
  });
  expect(file.ok()).toBeTruthy();
  await page.goto("/adhesion");
  await expect(page.locator("input[type=password]")).toHaveCount(0);
  await page.getByLabel("Nom", { exact: false }).first().fill("CandidatDémo");
  await page.getByLabel("Prénom").fill("Salma");
  await page.getByLabel("Adresse e-mail").fill(email);
  await page
    .getByLabel("Motivation et projet")
    .fill("Contribution aux recherches en intelligence artificielle du LIAS.");
  await page.locator("input[type=file]").setInputFiles({
    name: "CV-demo.pdf",
    mimeType: "application/pdf",
    buffer: await file.body(),
  });
  await page.getByRole("button", { name: "Soumettre ma candidature" }).click();
  await expect(
    page.getByText(
      "Votre demande a été enregistrée et sera étudiée par la direction.",
    ),
  ).toBeVisible();
  await login(page, "directeur");
  await page.goto("/adhesions");
  const row = page.getByRole("row").filter({ hasText: email });
  await row.getByRole("button", { name: "Examiner" }).click();
  await expect(
    page.getByRole("button", { name: "Télécharger le CV" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Confirmer la décision" }).click();
  await expect(row.getByText("Acceptee", { exact: true })).toBeVisible();
  const panel = page.getByRole("region", {
    name: "Invitation du nouveau membre",
  });
  await expect(
    panel.getByRole("button", { name: "Copier le lien" }),
  ).toBeVisible();
  const link = await panel.getByLabel("Lien d’activation").inputValue();
  await context.grantPermissions(["clipboard-read", "clipboard-write"]);
  await panel.getByRole("button", { name: "Copier le lien" }).click();
  await expect(panel.getByRole("status")).toHaveText("Lien copié.");
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(link);
  await expect(
    panel.getByRole("link", { name: "Ouvrir la page d’activation" }),
  ).toHaveAttribute("href", link);
  expect(new URL(link).pathname).toBe("/activation");
  expect(databaseState(email)).toMatchObject({
    etat: "ACTIF",
    admin: false,
    passwordAbsent: true,
    tokenPresent: true,
    expirationPresent: true,
    affiliations: 1,
    statut: "DOCTORANT",
    demande: "ACCEPTEE",
    membreLie: true,
    decisionPresente: true,
  });
  await page.reload();
  await row.getByRole("button", { name: "Lien d’activation" }).click();
  await expect(page.getByLabel("Lien d’activation")).toHaveValue(link);
  await page.getByRole("button", { name: "Se déconnecter" }).click();
  await page.goto(link);
  await page.getByLabel("Mot de passe (8 caractères minimum)").fill("CompteDemo2026!");
  await page.getByLabel("Confirmer le mot de passe").fill("CompteDemo2026!");
  const activated = page.waitForResponse(
    (r) =>
      r.url().endsWith("/api/auth/activer") && r.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Activer le compte" }).click();
  expect((await activated).status()).toBe(204);
  expect(databaseState(email)).toMatchObject({
    passwordAbsent: false,
    tokenPresent: false,
    expirationPresent: false,
    affiliations: 1,
    statut: "DOCTORANT",
  });
  const reused = await request.post("/api/auth/activer", {
    data: {
      token: new URL(link).searchParams.get("token"),
      motDePasse: "CompteDemo2026!",
    },
  });
  expect(reused.status()).toBe(400);
  await expect(page.getByText("Compte activé.")).toBeVisible();
  await page.getByRole("link", { name: "Se connecter", exact: true }).click();
  await page.getByLabel("Adresse e-mail").fill(email);
  await page.getByLabel("Mot de passe").fill("CompteDemo2026!");
  await page.getByRole("button", { name: "Se connecter", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Bonjour Salma," }),
  ).toBeVisible();
});

test("création événement, publication et recherche via les formulaires", async ({
  page,
}) => {
  await login(page, "directeur");
  const suffix = Date.now();
  await page.goto("/evenements");
  await page.getByRole("button", { name: "Nouvel événement" }).click();
  await page.getByLabel("Titre").fill("Séminaire démonstration " + suffix);
  await page.getByLabel("Type").first().selectOption("SEMINAIRE");
  await page.getByLabel("Lieu").fill("LIAS Casablanca");
  await page.getByLabel("Organisateurs").fill("2,3");
  await page.getByRole("button", { name: "Enregistrer", exact: true }).click();
  await expect(
    page.getByRole("cell", { name: "Séminaire démonstration " + suffix }),
  ).toBeVisible();
  await page.goto("/publications");
  await page.getByRole("button", { name: "Ajouter une publication" }).click();
  await page.getByLabel("Titre").fill("Article démonstration " + suffix);
  await page.getByLabel("Type").first().selectOption("ARTICLE_JOURNAL");
  await page.getByLabel("Auteurs dans l’ordre").fill("3,4,2");
  await page.getByRole("button", { name: "Enregistrer", exact: true }).click();
  await expect(
    page.getByRole("cell", { name: "Article démonstration " + suffix }),
  ).toBeVisible();
  await page.goto("/recherche");
  await page
    .getByRole("textbox", { name: "Recherche", exact: true })
    .fill(String(suffix));
  await page.getByRole("button", { name: "Rechercher", exact: true }).click();
  await expect(
    page.getByRole("link", { name: "Article démonstration " + suffix }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Séminaire démonstration " + suffix }),
  ).toBeVisible();
});
