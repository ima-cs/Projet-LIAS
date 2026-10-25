package ma.fsbm.lias;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import ma.fsbm.lias.domain.*;
import ma.fsbm.lias.domain.enums.*;
import ma.fsbm.lias.module.auth.CompteService;
import ma.fsbm.lias.module.historique.HistoriqueService;
import ma.fsbm.lias.module.membre.MembreService;
import ma.fsbm.lias.module.science.PdfService;
import ma.fsbm.lias.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "demo", "test" })
@Transactional
class ApplicationIntegrationTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper json;

  @Autowired
  UtilisateurRepository users;

  @Autowired
  MembreRepository members;

  @Autowired
  HistoriqueStatutRepository statuses;

  @Autowired
  HistoriqueRoleRepository roles;

  @Autowired
  MandatRepository mandates;

  @Autowired
  AffiliationRepository affiliations;

  @Autowired
  DocumentRepository documents;

  @Autowired
  DemandeAdhesionRepository admissions;

  @Autowired
  RapportAnnuelRepository reports;

  @Autowired
  MaterielRepository stock;

  @Autowired
  CompteService accounts;

  @Autowired
  MembreService profiles;

  @Autowired
  HistoriqueService history;

  @Autowired
  EntityManager em;

  @Autowired
  PdfService pdf;

  String token(String who) throws Exception {
    return json
      .readTree(
        mvc
          .perform(
            post("/api/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(
                json.writeValueAsString(
                  Map.of(
                    "email",
                    who + "@lias.ma",
                    "motDePasse",
                    "LiasDemo2026!"
                  )
                )
              )
          )
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString()
      )
      .get("accessToken")
      .asText();
  }

  Long id(String who) {
    return members
      .findByUtilisateur_Id(
        users
          .findByEmailIgnoreCase(who + "@lias.ma")
          .orElseThrow()
          .getId()
      )
      .orElseThrow()
      .getId();
  }

  JsonNode request(String url, Object body, String token) throws Exception {
    return json.readTree(
      mvc
        .perform(
          post(url)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body))
        )
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()
    );
  }

  @Test
  void activeLoginMeAndAuthorities() throws Exception {
    String t = token("directeur");
    mvc
      .perform(get("/api/auth/me").header("Authorization", "Bearer " + t))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.autorites[0]").value("ROLE_PERMANENT"))
      .andExpect(jsonPath("$.autorites[1]").value("ROLE_DIRECTEUR"));
    assertNotNull(
      users
        .findByEmailIgnoreCase("directeur@lias.ma")
        .orElseThrow()
        .getDerniereConnexion()
    );
  }

  @Test
  void wrongCredentialsAreGeneric() throws Exception {
    for (String email : List.of("directeur@lias.ma", "absent@example.ma"))
      mvc
        .perform(
          post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
              json.writeValueAsString(
                Map.of("email", email, "motDePasse", "wrong")
              )
            )
        )
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Identifiants invalides."));
  }

  @Test
  void frozenAndDisabledCannotLogin() throws Exception {
    for (var state : List.of(EtatCompte.GELE, EtatCompte.DESACTIVE)) {
      users
        .findByEmailIgnoreCase("permanent@lias.ma")
        .orElseThrow()
        .setEtatCompte(state);
      em.flush();
      mvc
        .perform(
          post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
              "{\"email\":\"permanent@lias.ma\",\"motDePasse\":\"LiasDemo2026!\"}"
            )
        )
        .andExpect(status().isForbidden());
    }
  }

  @Test
  void frozenAccountInvalidatesExistingJwt() throws Exception {
    var t = token("permanent");
    users
      .findByEmailIgnoreCase("permanent@lias.ma")
      .orElseThrow()
      .setEtatCompte(EtatCompte.GELE);
    em.flush();
    mvc
      .perform(get("/api/auth/me").header("Authorization", "Bearer " + t))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void activationIsSingleUse() throws Exception {
    var u = accounts.inviter("activation-test@example.ma");
    profiles.creer(u, "Test", "Invitation");
    var body = Map.of(
      "token",
      u.getTokenActivation(),
      "motDePasse",
      "Password2026!"
    );
    mvc
      .perform(
        post("/api/auth/activer")
          .contentType(MediaType.APPLICATION_JSON)
          .content(json.writeValueAsString(body))
      )
      .andExpect(status().isNoContent());
    assertNull(u.getTokenActivation());
    mvc
      .perform(
        post("/api/auth/activer")
          .contentType(MediaType.APPLICATION_JSON)
          .content(json.writeValueAsString(body))
      )
      .andExpect(status().isBadRequest());
    mvc
      .perform(
        post("/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"email\":\"activation-test@example.ma\",\"motDePasse\":\"Password2026!\"}"
          )
      )
      .andExpect(status().isOk());
  }

  @Test
  void expiredActivationRejected() throws Exception {
    var u = accounts.inviter("expired-test@example.ma");
    u.setTokenExpiration(LocalDateTime.now().minusDays(1));
    mvc
      .perform(
        post("/api/auth/activer")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            json.writeValueAsString(
              Map.of(
                "token",
                u.getTokenActivation(),
                "motDePasse",
                "Password2026!"
              )
            )
          )
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void nonAdminCannotAdministerAccounts() throws Exception {
    mvc
      .perform(
        patch("/api/admin/comptes/1/etat")
          .header("Authorization", "Bearer " + token("directeur"))
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"etat\":\"GELE\"}")
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void memberCanEditSelfButNotAnother() throws Exception {
    String t = token("permanent");
    var body =
      "{\"nom\":\"El Amrani\",\"prenom\":\"Youssef\",\"biographie\":\"Profil mis à jour\",\"dateNaissance\":\"1980-02-03\",\"centresInteret\":[\"IA\"]}";
    mvc
      .perform(
        put("/api/membres/" + id("permanent"))
          .header("Authorization", "Bearer " + t)
          .contentType(MediaType.APPLICATION_JSON)
          .content(body)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.biographie").value("Profil mis à jour"));
    mvc
      .perform(
        put("/api/membres/" + id("doctorant"))
          .header("Authorization", "Bearer " + t)
          .contentType(MediaType.APPLICATION_JSON)
          .content(body)
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void privateBirthDateNeverLeaks() throws Exception {
    members
      .findById(id("permanent"))
      .orElseThrow()
      .setDateNaissance(LocalDate.of(1980, 2, 3));
    em.flush();
    mvc
      .perform(get("/api/public/membres/" + id("permanent")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.dateNaissance").doesNotExist())
      .andExpect(jsonPath("$.motDePasseHash").doesNotExist());
    mvc
      .perform(
        get("/api/membres/" + id("permanent")).header(
          "Authorization",
          "Bearer " + token("doctorant")
        )
      )
      .andExpect(jsonPath("$.dateNaissance").doesNotExist());
    mvc
      .perform(
        get("/api/membres/" + id("permanent")).header(
          "Authorization",
          "Bearer " + token("permanent")
        )
      )
      .andExpect(jsonPath("$.dateNaissance").value("1980-02-03"));
  }

  @Test
  void memberFiltersUseCurrentPeriods() throws Exception {
    String t = token("permanent");
    mvc
      .perform(
        get("/api/membres?statut=PERMANENT&equipeId=1").header(
          "Authorization",
          "Bearer " + t
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  void invalidPhotoRejected() throws Exception {
    var f = new MockMultipartFile(
      "file",
      "virus.exe",
      "application/octet-stream",
      new byte[] { 1, 2 }
    );
    mvc
      .perform(
        multipart("/api/membres/" + id("permanent") + "/photo")
          .file(f)
          .with(r -> {
            r.setMethod("PUT");
            return r;
          })
          .header("Authorization", "Bearer " + token("permanent"))
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void temporalChangesPreserveAndReconstruct() throws Exception {
    Long id = id("permanent");
    long before = statuses.count();
    history.statut(id, TypeStatut.ASSOCIE, LocalDate.of(2022, 1, 1));
    em.flush();
    assertEquals(before + 1, statuses.count());
    assertEquals(
      "PERMANENT",
      history.etat(id, LocalDate.of(2021, 6, 1)).get("statut")
    );
    assertEquals(
      "ASSOCIE",
      history.etat(id, LocalDate.of(2022, 6, 1)).get("statut")
    );
  }

  @Test
  void secondAffiliationConflicts() throws Exception {
    mvc
      .perform(
        post("/api/membres/" + id("permanent") + "/affiliation")
          .header("Authorization", "Bearer " + token("admin"))
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"action\":\"RETOUR\",\"date\":\"2026-01-01\",\"type\":\"PERMANENT\"}"
          )
      )
      .andExpect(status().isConflict());
  }

  @Test
  void departureAndReturnKeepHistoryAndRestoreAccount() {
    Long id = id("permanent");
    history.affiliation(id, "DEPART", LocalDate.of(2022, 1, 1), null);
    em.flush();
    assertEquals(
      EtatCompte.DESACTIVE,
      members.findById(id).orElseThrow().getUtilisateur().getEtatCompte()
    );
    history.affiliation(
      id,
      "RETOUR",
      LocalDate.of(2023, 1, 1),
      TypeStatut.PERMANENT
    );
    em.flush();
    assertEquals(
      EtatCompte.ACTIF,
      members.findById(id).orElseThrow().getUtilisateur().getEtatCompte()
    );
    assertEquals(
      2,
      affiliations
        .findAll()
        .stream()
        .filter(a -> a.getMembre().getId().equals(id))
        .count()
    );
    assertEquals(
      "ANCIEN",
      history.etat(id, LocalDate.of(2022, 6, 1)).get("statut")
    );
  }

  @Test
  void retirementFreezesAccount() {
    Long id = id("permanent");
    history.statut(id, TypeStatut.RETRAITE, LocalDate.of(2023, 1, 1));
    em.flush();
    assertEquals(
      EtatCompte.GELE,
      members.findById(id).orElseThrow().getUtilisateur().getEtatCompte()
    );
    assertTrue(
      affiliations
        .findAll()
        .stream()
        .filter(a -> a.getMembre().getId().equals(id))
        .noneMatch(a -> a.getDateFin() == null)
    );
  }

  @Test
  void governanceRequiresPermanent() throws Exception {
    mvc
      .perform(
        post("/api/membres/" + id("doctorant") + "/roles")
          .header("Authorization", "Bearer " + token("directeur"))
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"type\":\"DIRECTEUR\",\"mandatId\":1,\"dateDebut\":\"2026-01-01\"}"
          )
      )
      .andExpect(status().isConflict());
  }

  @Test
  void teamRoleRequiresTeam() throws Exception {
    mvc
      .perform(
        post("/api/membres/" + id("permanent") + "/roles")
          .header("Authorization", "Bearer " + token("directeur"))
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"type\":\"CHEF_EQUIPE\",\"mandatId\":1,\"dateDebut\":\"2026-01-01\"}"
          )
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void closingMandateRevokesExistingTokenAndKeepsRoles() throws Exception {
    var t = token("directeur");
    long count = roles.count();
    history.closeMandat(mandates.findAll().getFirst().getId(), LocalDate.now());
    em.flush();
    assertEquals(count, roles.count());
    mvc
      .perform(get("/api/auth/me").header("Authorization", "Bearer " + t))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.autorites.length()").value(1));
    mvc
      .perform(get("/api/adhesions").header("Authorization", "Bearer " + t))
      .andExpect(status().isForbidden());
  }

  @Test
  void admissionAcceptCreatesCompleteAccount() throws Exception {
    String email = "candidate-" + UUID.randomUUID() + "@example.ma";
    var d = json.readTree(
      mvc
        .perform(
          post("/api/public/adhesions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
              json.writeValueAsString(
                Map.of(
                  "nom",
                  "Candidate",
                  "prenom",
                  "Lina",
                  "email",
                  email,
                  "motivation",
                  "Recherche IA"
                )
              )
            )
        )
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()
    );
    var result = request(
      "/api/adhesions/" + d.get("id").asLong() + "/decision",
      Map.of("accepter", true, "statut", "DOCTORANT"),
      token("directeur")
    );
    assertEquals("ACCEPTEE", result.get("statutDemande").asText());
    var u = users.findByEmailIgnoreCase(email).orElseThrow();
    assertNull(u.getMotDePasseHash());
    assertNotNull(u.getTokenActivation());
    assertEquals(EtatCompte.ACTIF, u.getEtatCompte());
    assertFalse(u.isEstAdmin());
    assertTrue(u.getTokenExpiration().isAfter(LocalDateTime.now()));
    assertEquals(
      "http://127.0.0.1:5173/activation?token=" + u.getTokenActivation(),
      result.get("activationUrl").asText()
    );
    Long createdId = result.get("membreCreeId").asLong();
    assertEquals(
      u.getId(),
      members.findById(createdId).orElseThrow().getUtilisateur().getId()
    );
    assertEquals(
      1,
      affiliations
        .findAll()
        .stream()
        .filter(
          a -> a.getMembre().getId().equals(createdId) && a.getDateFin() == null
        )
        .count()
    );
    var accepted = admissions.findById(d.get("id").asLong()).orElseThrow();
    assertNotNull(accepted.getDateDecision());
    assertEquals(id("directeur"), accepted.getTraitePar().getId());
    assertEquals(
      "DOCTORANT",
      history
        .etat(result.get("membreCreeId").asLong(), LocalDate.now())
        .get("statut")
    );
  }

  @Test
  void invitationReusesOnlyAnUnattachedUnactivatedAccount() {
    var existing = accounts.inviter("unattached@example.ma");
    var oldToken = existing.getTokenActivation();
    var reused = accounts.inviter("unattached@example.ma");
    assertEquals(existing.getId(), reused.getId());
    assertNotEquals(oldToken, reused.getTokenActivation());
    assertEquals(EtatCompte.ACTIF, reused.getEtatCompte());
    assertNull(reused.getMotDePasseHash());
    assertFalse(reused.isEstAdmin());
    assertThrows(ma.fsbm.lias.common.error.ConflictException.class, () ->
      accounts.inviter("permanent@lias.ma")
    );
  }

  @Test
  void admissionRefusalRequiresReasonAndIsPreserved() throws Exception {
    var d = admissions.findAll().getFirst();
    var t = token("directeur");
    mvc
      .perform(
        post("/api/adhesions/" + d.getId() + "/decision")
          .header("Authorization", "Bearer " + t)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"accepter\":false}")
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void eventPublicationAndNotificationWorkflow() throws Exception {
    String t = token("directeur");
    var event = request(
      "/api/evenements",
      Map.of(
        "titre",
        "Séminaire test",
        "typeEvenement",
        "SEMINAIRE",
        "dateDebut",
        LocalDate.now().toString(),
        "lieu",
        "LIAS",
        "organisateurIds",
        List.of(id("permanent"))
      ),
      t
    );
    assertTrue(event.get("id").asLong() > 0);
    var pub = request(
      "/api/publications",
      Map.of(
        "titre",
        "Article test",
        "typePublication",
        "ARTICLE_JOURNAL",
        "annee",
        2026,
        "auteurIds",
        List.of(id("permanent"), id("doctorant"))
      ),
      t
    );
    assertEquals(
      id("permanent"),
      pub.get("auteurs").get(0).get("membreId").asLong()
    );
    mvc
      .perform(get("/api/notifications").header("Authorization", "Bearer " + t))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].type").value("EVENEMENT_CREE"));
  }

  @Test
  void documentUploadDownloadAndVersion() throws Exception {
    String t = token("permanent");
    byte[] bytes = pdf.create(List.of("Document test"));
    var file = new MockMultipartFile(
      "file",
      "test.pdf",
      "application/pdf",
      bytes
    );
    var first = json.readTree(
      mvc
        .perform(
          multipart("/api/documents")
            .file(file)
            .param("type", "AUTRE")
            .header("Authorization", "Bearer " + t)
        )
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()
    );
    mvc
      .perform(
        get(
          "/api/documents/" + first.get("id").asLong() + "/telecharger"
        ).header("Authorization", "Bearer " + t)
      )
      .andExpect(status().isOk())
      .andExpect(content().bytes(bytes));
    mvc
      .perform(
        multipart("/api/documents")
          .file(file)
          .param("type", "AUTRE")
          .param("parentId", first.get("id").asText())
          .header("Authorization", "Bearer " + t)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.version").value(2));
  }

  @Test
  void restrictedModulesRejectAssociateAndDoctorant() throws Exception {
    for (String role : List.of("associe", "doctorant")) {
      String t = token(role);
      for (String p : List.of(
        "documents",
        "materiels",
        "conventions",
        "reunions",
        "rapports",
        "adhesions"
      ))
        mvc
          .perform(get("/api/" + p).header("Authorization", "Bearer " + t))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void stockDistributionCannotOverdraw() throws Exception {
    String t = token("directeur");
    var item = stock.findAll().getFirst();
    int before = item.getQuantiteDisponible();
    mvc
      .perform(
        post("/api/distributions")
          .header("Authorization", "Bearer " + t)
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            json.writeValueAsString(
              Map.of(
                "materielId",
                item.getId(),
                "membreId",
                id("permanent"),
                "quantite",
                before + 1,
                "dateDistribution",
                LocalDate.now().toString()
              )
            )
          )
      )
      .andExpect(status().isConflict());
    assertEquals(before, item.getQuantiteDisponible());
    request(
      "/api/distributions",
      Map.of(
        "materielId",
        item.getId(),
        "membreId",
        id("permanent"),
        "quantite",
        1,
        "dateDistribution",
        LocalDate.now().toString()
      ),
      t
    );
    assertEquals(before - 1, item.getQuantiteDisponible());
  }

  @Test
  void conventionAndNeedWorkflow() throws Exception {
    String t = token("directeur");
    var partner = request(
      "/api/partenaires",
      Map.of(
        "nom",
        "Partenaire test",
        "typePartenaire",
        "ACADEMIQUE",
        "pays",
        "Maroc"
      ),
      t
    );
    request(
      "/api/conventions",
      Map.of(
        "titre",
        "Convention test",
        "partenaireId",
        partner.get("id").asLong(),
        "dateDebut",
        "2026-01-01",
        "statutConvention",
        "ACTIVE"
      ),
      t
    );
    var need = request(
      "/api/besoins",
      Map.of(
        "designation",
        "Ecran",
        "justification",
        "Recherche",
        "quantiteDemandee",
        1
      ),
      token("permanent")
    );
    mvc
      .perform(
        patch("/api/besoins/" + need.get("id").asLong() + "/decision")
          .header("Authorization", "Bearer " + t)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"statut\":\"VALIDE\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.statutBesoin").value("VALIDE"));
  }

  @Test
  void reportIsPdfAndRegenerationKeepsOneAnnualRow() throws Exception {
    String t = token("directeur");
    long before = reports.count();
    var a = request("/api/rapports/2026/generer", Map.of(), t);
    var b = request("/api/rapports/2026/generer", Map.of(), t);
    assertEquals(a.get("id"), b.get("id"));
    assertEquals(before + 1, reports.count());
    mvc
      .perform(
        get(
          "/api/documents/" + b.get("documentId").asLong() + "/telecharger"
        ).header("Authorization", "Bearer " + t)
      )
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/pdf"));
    var file = documents.findById(b.get("documentId").asLong()).orElseThrow();
    assertEquals(2, file.getVersion());
  }

  @Test
  void forgedMimeIsRejected() throws Exception {
    var file = new MockMultipartFile(
      "file",
      "fake.pdf",
      "application/pdf",
      "executable".getBytes()
    );
    mvc
      .perform(
        multipart("/api/documents")
          .file(file)
          .param("type", "AUTRE")
          .header("Authorization", "Bearer " + token("permanent"))
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void publicAdmissionCanArchiveAnOptionalCv() throws Exception {
    var data = new MockMultipartFile(
      "demande",
      "demande.json",
      "application/json",
      json.writeValueAsBytes(
        Map.of(
          "nom",
          "CVTest",
          "prenom",
          "Lina",
          "email",
          "cvtest@example.ma",
          "motivation",
          "Recherche"
        )
      )
    );
    var cv = new MockMultipartFile(
      "file",
      "cv.pdf",
      "application/pdf",
      pdf.create(List.of("Curriculum vitae"))
    );
    var result = json.readTree(
      mvc
        .perform(multipart("/api/public/adhesions").file(data).file(cv))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()
    );
    var saved = admissions.findById(result.get("id").asLong()).orElseThrow();
    assertNotNull(saved.getCv());
    assertEquals(TypeDocument.CV, saved.getCv().getTypeDocument());
  }

  @Test
  void teamDiscussionIsRestrictedToCurrentTeam() throws Exception {
    var thread = request(
      "/api/discussions",
      Map.of(
        "titre",
        "Équipe privée",
        "typeDiscussion",
        "EQUIPE",
        "equipeId",
        1
      ),
      token("permanent")
    );
    var url = "/api/discussions/" + thread.get("id").asLong() + "/messages";
    request(
      url,
      Map.of("contenu", "Résultats de recherche"),
      token("doctorant")
    );
    mvc
      .perform(get(url).header("Authorization", "Bearer " + token("associe")))
      .andExpect(status().isForbidden());
    mvc
      .perform(get(url).header("Authorization", "Bearer " + token("permanent")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].contenu").value("Résultats de recherche"));
  }

  @Test
  void notificationCannotBeReadByAnotherRecipient() throws Exception {
    String t = token("directeur");
    var list = json.readTree(
      mvc
        .perform(
          get("/api/notifications").header("Authorization", "Bearer " + t)
        )
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString()
    );
    mvc
      .perform(
        patch(
          "/api/notifications/" + list.get(0).get("id").asLong() + "/lire"
        ).header("Authorization", "Bearer " + token("doctorant"))
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void publicAndAuthenticationBoundary() throws Exception {
    mvc
      .perform(get("/actuator/health"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("UP"));
    mvc
      .perform(get("/api/public/portail"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.equipes.length()").value(2));
    mvc.perform(get("/api/membres")).andExpect(status().isUnauthorized());
  }
}
