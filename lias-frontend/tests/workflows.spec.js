import { test, expect } from "@playwright/test";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

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
  const outbox = fileURLToPath(
    new URL("../../lias-backend/storage/invitations-demo.txt", import.meta.url),
  );
  await expect
    .poll(async () => {
      try {
        return (await readFile(outbox, "utf8")).includes(email);
      } catch {
        return false;
      }
    })
    .toBe(true);
  const text = await readFile(outbox, "utf8");
  const link = text.split(email + "\n")[1].split("\n")[0];
  await page.getByRole("button", { name: "Se déconnecter" }).click();
  await page.goto(link);
  await page.getByLabel("Mot de passe").fill("CompteDemo2026!");
  await page.getByRole("button", { name: "Activer le compte" }).click();
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
