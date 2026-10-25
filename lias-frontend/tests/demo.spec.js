import { test, expect } from "@playwright/test";

test("interface publique et dashboard sur mobile", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: /La recherche/ }),
  ).toBeVisible();
  await page.screenshot({
    path: "test-results/previews/accueil-mobile.png",
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth + 1,
    ),
  ).toBe(true);
  await login(page, "directeur");
  await expect(page.getByRole("heading", { name: /Bonjour/ })).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth + 1,
    ),
  ).toBe(true);
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.screenshot({
    path: "test-results/previews/dashboard.png",
    fullPage: true,
  });
});
async function login(page, role) {
  await page.goto("/connexion");
  await page.getByLabel("Adresse e-mail").fill(role + "@lias.ma");
  await page.getByLabel("Mot de passe").fill("LiasDemo2026!");
  await page.getByRole("button", { name: "Se connecter", exact: true }).click();
  await expect(page.getByRole("heading", { name: /Bonjour/ })).toBeVisible();
}
test("portail public, équipes, membres, événements et publications", async ({
  page,
}) => {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: /La recherche/ }),
  ).toBeVisible();
  for (const url of ["/equipes", "/membres", "/evenements", "/publications"]) {
    await page.goto(url);
    await expect(page.locator("main .panel").first()).toBeVisible();
    await expect(page.locator("[role=alert]")).toHaveCount(0);
  }
  await page.goto("/membres/3");
  await expect(
    page.getByRole("heading", { name: "Youssef El Amrani" }),
  ).toBeVisible();
  await expect(page.getByText("Date de naissance (privée)")).toHaveCount(0);
  expect(errors).toEqual([]);
});
test("directeur : navigation métier et téléchargement rapport PDF", async ({
  page,
}) => {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await login(page, "directeur");
  for (const url of [
    "/adhesions",
    "/equipes",
    "/membres",
    "/evenements",
    "/publications",
    "/documents",
    "/reunions",
    "/materiel",
    "/besoins",
    "/partenaires",
    "/conventions",
    "/discussions",
    "/notifications",
    "/gouvernance",
  ]) {
    await page.goto(url);
    await expect(page.locator("main h1")).toBeVisible();
    await expect(page.getByText("Chargement…", { exact: true })).toHaveCount(0);
    await expect(page.locator("[role=alert]")).toHaveCount(0);
  }
  await page.goto("/rapports");
  await page
    .getByRole("button", { name: "Générer le rapport", exact: true })
    .click();
  await expect(
    page.getByRole("button", { name: "Télécharger PDF" }),
  ).toBeVisible();
  const dl = page.waitForEvent("download");
  await page.getByRole("button", { name: "Télécharger PDF" }).click();
  const result = await dl;
  expect(result.suggestedFilename()).toContain(".pdf");
  expect(errors).toEqual([]);
});
test("permanent : profil, calendrier, besoin matériel et discussion", async ({
  page,
}) => {
  await login(page, "permanent");
  await page.goto("/profil");
  await page.getByRole("button", { name: "Modifier mon profil" }).click();
  await page
    .getByLabel("Biographie")
    .fill(
      "Chercheur au LIAS. Recherche collaborative en intelligence artificielle.",
    );
  await page.getByRole("button", { name: "Enregistrer", exact: true }).click();
  await expect(
    page.getByText("Profil enregistré. Une révision a été créée."),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Historique des modifications" })
    .click();
  await expect(
    page.getByRole("columnheader", { name: "Révision" }),
  ).toBeVisible();
  await page.goto("/calendrier");
  await expect(
    page.getByText("Journée scientifique LIAS", { exact: true }),
  ).toBeVisible();
  await page.goto("/besoins");
  await page.getByRole("button", { name: "Exprimer un besoin" }).click();
  await page.getByLabel("Désignation").fill("Écran recherche démonstration");
  await page.getByLabel("Quantité").fill("1");
  await page
    .getByLabel("Justification")
    .fill("Travaux du laboratoire et analyse de données.");
  await page.getByRole("button", { name: "Enregistrer", exact: true }).click();
  await expect(
    page
      .getByRole("cell", { name: "Écran recherche démonstration", exact: true })
      .first(),
  ).toBeVisible();
  await page.goto("/discussions");
  await page.getByRole("button", { name: /Vie du laboratoire/ }).click();
  await expect(page.getByRole("heading", { name: "Échanges" })).toBeVisible();
  await page.getByLabel("Votre message").fill("Bonjour à toute l’équipe LIAS.");
  await page.getByRole("button", { name: "Envoyer", exact: true }).click();
  await expect(
    page
      .locator(".message p")
      .filter({ hasText: "Bonjour à toute l’équipe LIAS." })
      .first(),
  ).toBeVisible();
});
test("administrateur et séparation des droits métier", async ({ page }) => {
  await login(page, "admin");
  await page.goto("/comptes");
  await expect(
    page.getByRole("cell", { name: "directeur@lias.ma" }),
  ).toBeVisible();
  await page.goto("/gouvernance");
  await expect(
    page.getByRole("heading", { name: "Gouvernance & historisation" }),
  ).toBeVisible();
  await page.goto("/materiel");
  await expect(
    page.getByRole("heading", { name: "Accès réservé" }),
  ).toBeVisible();
});
test("associé : consultation et absence des modules internes", async ({
  page,
}) => {
  await login(page, "associe");
  await page.goto("/publications");
  await expect(
    page.getByRole("cell", {
      name: "Apprentissage fédéré pour les systèmes de santé",
    }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Inventaire", exact: true }),
  ).toHaveCount(0);
  await page.goto("/documents");
  await expect(
    page.getByRole("heading", { name: "Accès réservé" }),
  ).toBeVisible();
});
