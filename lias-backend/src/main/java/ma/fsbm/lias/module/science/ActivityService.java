package ma.fsbm.lias.module.science;

import static ma.fsbm.lias.common.Views.of;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.common.CurrentUser;
import ma.fsbm.lias.domain.*;
import ma.fsbm.lias.domain.enums.*;
import ma.fsbm.lias.module.historique.HistoriqueService;
import ma.fsbm.lias.module.membre.MembreService;
import ma.fsbm.lias.module.transversal.BusinessEvent;
import ma.fsbm.lias.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityService {

  private final ReunionRepository meetings;
  private final RapportAnnuelRepository reports;
  private final DocumentService documents;
  private final ScienceService science;
  private final MembreService members;
  private final HistoriqueService history;
  private final ConventionRepository conventions;
  private final CurrentUser user;
  private final PdfService pdf;
  private final ApplicationEventPublisher publisher;

  public record Meeting(
    String titre,
    LocalDate dateReunion,
    String ordreDuJour
  ) {}

  public Map<String, Object> meetingView(Reunion r) {
    return of(
      "id",
      r.getId(),
      "titre",
      r.getTitre(),
      "dateReunion",
      r.getDateReunion(),
      "ordreDuJour",
      r.getOrdreDuJour(),
      "createurId",
      r.getCreePar().getId(),
      "pvDocumentId",
      r.getPvDocument() == null ? null : r.getPvDocument().getId()
    );
  }

  public List<Map<String, Object>> meetings() {
    user.internal();
    return meetings.findAll().stream().map(this::meetingView).toList();
  }

  public Map<String, Object> meeting(Meeting req) {
    user.internal();
    if (
      req.titre() == null ||
      req.titre().isBlank() ||
      req.titre().length() > 200 ||
      req.dateReunion() == null
    ) throw new IllegalArgumentException("Titre et date requis");
    var r = new Reunion();
    r.setTitre(req.titre());
    r.setDateReunion(req.dateReunion());
    r.setOrdreDuJour(req.ordreDuJour());
    r.setCreePar(user.membre());
    return meetingView(meetings.save(r));
  }

  public Map<String, Object> pv(Long id, MultipartFile f) {
    user.internal();
    var r = meetings
      .findById(id)
      .orElseThrow(() ->
        new jakarta.persistence.EntityNotFoundException("Réunion introuvable")
      );
    if (
      !user.has("DIRECTEUR") &&
      !r.getCreePar().getId().equals(user.membre().getId())
    ) throw new org.springframework.security.access.AccessDeniedException(
      "Créateur requis"
    );
    if (
      !"application/pdf".equals(f.getContentType())
    ) throw new IllegalArgumentException("PV au format PDF requis");
    r.setPvDocument(
      documents.store(
        f,
        TypeDocument.PV,
        null,
        r.getPvDocument() == null ? null : r.getPvDocument().getId()
      )
    );
    return meetingView(r);
  }

  private Map<String, Object> reportView(RapportAnnuel r) {
    return of(
      "id",
      r.getId(),
      "annee",
      r.getAnnee(),
      "dateGeneration",
      r.getDateGeneration(),
      "documentId",
      r.getDocument() == null ? null : r.getDocument().getId()
    );
  }

  public List<Map<String, Object>> reports() {
    user.internal();
    return reports.findAll().stream().map(this::reportView).toList();
  }

  public Map<String, Object> generate(int year) {
    user.director();
    if (
      year < 1900 || year > LocalDate.now().getYear()
    ) throw new IllegalArgumentException("Année invalide");
    var r = reports
      .findAll()
      .stream()
      .filter(x -> x.getAnnee() == year)
      .findFirst()
      .orElseGet(RapportAnnuel::new);
    List<String> lines = new ArrayList<>();
    lines.add("LIAS - Rapport annuel d'activité " + year);
    lines.add("Généré le " + LocalDate.now());
    lines.add("");
    lines.add("MEMBRES ET GOUVERNANCE AU 31 DECEMBRE");
    var ms = (List<Map<String, Object>>) members
      .list(null, null, null, 0, 100, true)
      .get("content");
    for (var m : ms) {
      var state = history.etat((Long) m.get("id"), LocalDate.of(year, 12, 31));
      if (
        (
          (List<Map<String, Object>>) history
            .periodes((Long) m.get("id"))
            .get("affiliations")
        )
          .stream()
          .anyMatch(
            a ->
              !((LocalDate) a.get("dateDebut")).isAfter(
                LocalDate.of(year, 12, 31)
              ) &&
              (a.get("dateFin") == null ||
                ((LocalDate) a.get("dateFin")).isAfter(
                  LocalDate.of(year, 1, 1)
                ))
          )
      ) lines.add(
        m.get("prenom") +
          " " +
          m.get("nom") +
          " - " +
          state.get("statut") +
          " - " +
          state.getOrDefault("equipe", "") +
          " - " +
          state.get("roles") +
          " | Périodes : " +
          history.periodes((Long) m.get("id"))
      );
    }
    lines.add("");
    lines.add("EVENEMENTS");
    science
      .events(null, null, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
      .forEach(e ->
        lines.add(
          e.get("dateDebut") + " - " + e.get("titre") + " - " + e.get("lieu")
        )
      );
    lines.add("");
    lines.add("PUBLICATIONS");
    science
      .publications(null, year, null, null, null)
      .forEach(p ->
        lines.add(
          p.get("titre") +
            " - " +
            p.get("typePublication") +
            " - " +
            p.get("auteurs")
        )
      );
    lines.add("");
    lines.add("REUNIONS ET ACTIVITES");
    meetings
      .findAll()
      .stream()
      .filter(m -> m.getDateReunion().getYear() == year)
      .forEach(m ->
        lines.add(
          m.getDateReunion() + " - " + m.getTitre() + " : " + m.getOrdreDuJour()
        )
      );
    lines.add("");
    lines.add("CONVENTIONS");
    conventions
      .findAll()
      .stream()
      .filter(
        c ->
          c.getDateDebut().getYear() <= year &&
          (c.getDateFin() == null || c.getDateFin().getYear() >= year)
      )
      .forEach(c ->
        lines.add(c.getTitre() + " - " + c.getPartenaire().getNom())
      );
    var d = documents.store(
      new GeneratedFile("Rapport-LIAS-" + year + ".pdf", pdf.create(lines)),
      TypeDocument.RAPPORT,
      null,
      r.getDocument() == null ? null : r.getDocument().getId()
    );
    r.setAnnee(year);
    r.setDateGeneration(LocalDateTime.now());
    r.setDocument(d);
    r.setGenerePar(user.membre());
    reports.saveAndFlush(r);
    publisher.publishEvent(
      new BusinessEvent(
        TypeNotification.RAPPORT_GENERE,
        "Rapport annuel généré",
        "Année " + year,
        "/rapports",
        null,
        false
      )
    );
    return reportView(r);
  }
}
