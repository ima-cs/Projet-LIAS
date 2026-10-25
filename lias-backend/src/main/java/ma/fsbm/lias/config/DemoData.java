package ma.fsbm.lias.config;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.domain.*;
import ma.fsbm.lias.domain.enums.*;
import ma.fsbm.lias.module.collaboration.CollaborationService;
import ma.fsbm.lias.module.gouvernance.AdhesionService;
import ma.fsbm.lias.module.historique.HistoriqueService;
import ma.fsbm.lias.module.materiel.MaterielService;
import ma.fsbm.lias.module.membre.MembreService;
import ma.fsbm.lias.module.science.*;
import ma.fsbm.lias.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit opt-in demo data; natural-key checks allow safe repeated starts. */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoData implements CommandLineRunner {

  private final UtilisateurRepository users;
  private final MembreRepository members;
  private final PasswordEncoder passwords;
  private final MembreService memberService;
  private final HistoriqueService history;
  private final ScienceService science;
  private final EquipeRepository teams;
  private final MandatRepository mandates;
  private final LaboratoireRepository labs;
  private final HistoriqueRoleRepository roles;
  private final EvenementRepository events;
  private final PublicationRepository publications;
  private final DocumentRepository documents;
  private final DocumentService documentService;
  private final PdfService pdf;
  private final ReunionRepository meetings;
  private final ActivityService activity;
  private final MaterielRepository stock;
  private final MaterielService material;
  private final PartenaireRepository partners;
  private final ConventionRepository conventions;
  private final CollaborationService collaboration;
  private final DiscussionRepository discussions;
  private final DemandeAdhesionRepository adhesions;

  @Override
  @Transactional
  public void run(String... args) {
    try {
      if (labs.count() == 0) {
        var l = new Laboratoire();
        l.setNom("Laboratoire d’Informatique et d’Aide aux Systèmes");
        l.setSigle("LIAS");
        l.setDateCreationLabo(LocalDate.of(2010, 1, 1));
        l.setPresentation(
          "Le LIAS réunit chercheurs et doctorants autour de l’intelligence artificielle, des systèmes distribués et de la science des données. Notre laboratoire développe une recherche ouverte, collaborative et ancrée dans les enjeux de la société."
        );
        labs.save(l);
      }
      account("admin", "Administrateur", "Système", null, true);
      var director = account(
        "directeur",
        "Bennani",
        "Samira",
        TypeStatut.PERMANENT,
        false
      );
      var permanent = account(
        "permanent",
        "El Amrani",
        "Youssef",
        TypeStatut.PERMANENT,
        false
      );
      var doctorant = account(
        "doctorant",
        "Alaoui",
        "Imane",
        TypeStatut.DOCTORANT,
        false
      );
      var associe = account(
        "associe",
        "Idrissi",
        "Karim",
        TypeStatut.ASSOCIE,
        false
      );
      login("directeur@lias.ma", List.of("ROLE_PERMANENT", "ROLE_DIRECTEUR"));
      if (teams.count() == 0) {
        science.saveTeam(
          null,
          new ScienceService.TeamRequest(
            "Intelligence artificielle et données",
            "IA, apprentissage et données",
            "Des modèles intelligents au service de la recherche et de la société."
          )
        );
        science.saveTeam(
          null,
          new ScienceService.TeamRequest(
            "Systèmes et réseaux",
            "Systèmes distribués, cybersécurité",
            "Fiabilité, sécurité et performance des infrastructures numériques."
          )
        );
      }
      var ts = teams.findAll();
      var start = LocalDate.of(2020, 1, 1);
      for (var m : List.of(director, permanent, doctorant, associe))
        if (
          history.etat(m.getId(), LocalDate.now()).get("equipeId") == null &&
          history.etat(m.getId(), LocalDate.now()).get("statut") != null &&
          !Set.of("RETRAITE", "ANCIEN").contains(
            history.etat(m.getId(), LocalDate.now()).get("statut")
          )
        ) history.equipe(
          m.getId(),
          ts.get(m == associe ? 1 : 0).getId(),
          start
        );
      if (mandates.count() == 0) history.mandat(
        "Direction LIAS 2024–2028",
        LocalDate.of(2024, 1, 1)
      );
      if (
        roles
          .findAll()
          .stream()
          .noneMatch(
            r ->
              r.getMembre().getId().equals(director.getId()) &&
              r.getTypeRole() == TypeRole.DIRECTEUR
          )
      ) history.role(
        director.getId(),
        TypeRole.DIRECTEUR,
        null,
        mandates.findAll().getFirst().getId(),
        LocalDate.of(2024, 1, 1)
      );
      if (events.count() == 0) {
        science.saveEvent(
          null,
          new ScienceService.EventRequest(
            "Journée scientifique LIAS",
            TypeEvenement.CONFERENCE,
            "Rencontres des chercheurs, présentations des travaux et échanges avec nos partenaires.",
            "2026",
            LocalDate.now().plusDays(3),
            LocalDate.now().plusDays(3),
            "Faculté des Sciences Ben M’Sik",
            List.of(director.getId(), permanent.getId())
          )
        );
        science.saveEvent(
          null,
          new ScienceService.EventRequest(
            "Atelier IA responsable",
            TypeEvenement.WORKSHOP,
            "Expérimentation et bonnes pratiques en intelligence artificielle.",
            "1ère édition",
            LocalDate.now().plusDays(10),
            null,
            "Salle de conférences LIAS",
            List.of(permanent.getId())
          )
        );
        science.saveEvent(
          null,
          new ScienceService.EventRequest(
            "Séminaire systèmes distribués",
            TypeEvenement.SEMINAIRE,
            "Résultats et perspectives des projets du laboratoire.",
            "2026",
            LocalDate.now().minusDays(7),
            null,
            "Amphi A",
            List.of(associe.getId(), permanent.getId())
          )
        );
      }
      if (publications.count() == 0) {
        science.savePublication(
          null,
          new ScienceService.PubRequest(
            "Apprentissage fédéré pour les systèmes de santé",
            TypePublication.ARTICLE_JOURNAL,
            2026,
            "10.1000/lias.demo.2026",
            null,
            ts.getFirst().getId(),
            List.of(permanent.getId(), doctorant.getId(), director.getId())
          )
        );
        science.savePublication(
          null,
          new ScienceService.PubRequest(
            "Sécurité des infrastructures distribuées",
            TypePublication.ARTICLE_CONFERENCE,
            2025,
            null,
            null,
            ts.getLast().getId(),
            List.of(associe.getId(), permanent.getId())
          )
        );
      }
      if (documents.count() == 0) {
        var f = new GeneratedFile(
          "Programme-LIAS.pdf",
          pdf.create(
            List.of(
              "LIAS - Programme scientifique",
              "Présentation des équipes et des axes de recherche",
              "Séminaires, ateliers et conférences - 2026"
            )
          )
        );
        documentService.store(
          f,
          TypeDocument.PROGRAMME,
          events.findAll().getFirst().getId(),
          null
        );
      }
      if (meetings.count() == 0) {
        var r = activity.meeting(
          new ActivityService.Meeting(
            "Conseil du laboratoire",
            LocalDate.now().minusDays(2),
            "Bilan scientifique, préparation de la journée LIAS et besoins matériels."
          )
        );
        activity.pv(
          (Long) r.get("id"),
          new GeneratedFile(
            "PV-conseil-LIAS.pdf",
            pdf.create(
              List.of(
                "LIAS - Procès-verbal du conseil",
                "Décisions : validation du programme scientifique.",
                "Priorités : soutien aux doctorants et renouvellement du matériel."
              )
            )
          )
        );
      }
      if (stock.count() == 0) {
        var s = material.arrive(
          new MaterielService.Arrivage(
            "Ordinateur portable recherche",
            "16 Go RAM, SSD 512 Go",
            8,
            LocalDate.now().minusDays(30)
          )
        );
        material.distribute(
          new MaterielService.Distribution(
            (Long) s.get("id"),
            permanent.getId(),
            1,
            LocalDate.now().minusDays(10)
          )
        );
        material.need(
          new MaterielService.Besoin(
            "Station de calcul GPU",
            "Expériences d’apprentissage profond",
            1
          )
        );
      }
      if (partners.count() == 0) collaboration.partner(
        null,
        new CollaborationService.Partner(
          "Université Hassan II",
          TypePartenaire.ACADEMIQUE,
          "recherche@example.ma",
          "Maroc",
          "Coopération scientifique et encadrement doctoral."
        )
      );
      if (conventions.count() == 0) collaboration.agreement(
        null,
        new CollaborationService.Agreement(
          "Coopération recherche et innovation",
          partners.findAll().getFirst().getId(),
          LocalDate.of(2025, 1, 1),
          LocalDate.of(2028, 12, 31),
          StatutConvention.ACTIVE,
          documents.findAll().getFirst().getId()
        )
      );
      if (discussions.count() == 0) {
        var d = collaboration.thread(
          new CollaborationService.Thread(
            "Vie du laboratoire",
            TypeDiscussion.GENERALE,
            null,
            null
          )
        );
        collaboration.message(
          (Long) d.get("id"),
          "Bienvenue dans l’espace LIAS. Retrouvez ici les échanges et les nouvelles de notre laboratoire."
        );
      }
      if (adhesions.count() == 0) adhesionsSeed();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void adhesionsSeed() {
    var d = new DemandeAdhesion();
    d.setNom("Mansouri");
    d.setPrenom("Lina");
    d.setEmail("lina.mansouri@example.ma");
    d.setMotivation(
      "Je souhaite rejoindre l’équipe IA pour préparer une thèse sur l’apprentissage fédéré."
    );
    adhesions.save(d);
  }

  private Membre account(
    String key,
    String nom,
    String prenom,
    TypeStatut type,
    boolean admin
  ) {
    var existing = users.findByEmailIgnoreCase(key + "@lias.ma");
    if (existing.isPresent()) return members
      .findByUtilisateur_Id(existing.get().getId())
      .orElseThrow();
    var u = new Utilisateur();
    u.setEmail(key + "@lias.ma");
    u.setMotDePasseHash(passwords.encode("LiasDemo2026!"));
    u.setEstAdmin(admin);
    users.save(u);
    var m = memberService.creer(u, nom, prenom);
    m.setBiographie(
      "Membre du laboratoire LIAS, impliqué dans la recherche et le partage des connaissances."
    );
    m.setEtablissementOrigine("Faculté des Sciences Ben M’Sik");
    m.setLaboratoireOrigine("LIAS");
    m.setDateEmbauche(LocalDate.of(2020, 1, 1));
    m.setCentresInteret(new HashSet<>(List.of("Recherche", "Innovation")));
    if (type != null) history.initial(
      m.getId(),
      type,
      LocalDate.of(2020, 1, 1)
    );
    return m;
  }

  private void login(String email, List<String> authorities) {
    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken(
        email,
        null,
        authorities.stream().map(SimpleGrantedAuthority::new).toList()
      )
    );
  }
}
