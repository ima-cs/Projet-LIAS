package ma.fsbm.lias.module.science;

import static ma.fsbm.lias.common.Views.of;

import jakarta.persistence.*;
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

@Service
@RequiredArgsConstructor
@Transactional
public class ScienceService {

  private final EquipeRepository teams;
  private final EvenementRepository events;
  private final PublicationRepository pubs;
  private final PublicationAuteurRepository authors;
  private final MembreService members;
  private final HistoriqueService history;
  private final CurrentUser user;
  private final ApplicationEventPublisher publisher;

  public record TeamRequest(
    String nom,
    String thematique,
    String description
  ) {}

  public record EventRequest(
    String titre,
    TypeEvenement typeEvenement,
    String description,
    String edition,
    LocalDate dateDebut,
    LocalDate dateFin,
    String lieu,
    List<Long> organisateurIds
  ) {}

  public record PubRequest(
    String titre,
    TypePublication typePublication,
    Integer annee,
    String doi,
    String lien,
    Long equipeId,
    List<Long> auteurIds
  ) {}

  private void text(String s, int max) {
    if (
      s == null || s.isBlank() || s.length() > max
    ) throw new IllegalArgumentException("Texte obligatoire ou trop long");
  }

  public Equipe team(Long id) {
    return teams
      .findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Équipe introuvable"));
  }

  public Evenement event(Long id) {
    return events
      .findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Événement introuvable"));
  }

  public Map<String, Object> teamView(Equipe e) {
    return of(
      "id",
      e.getId(),
      "nom",
      e.getNom(),
      "thematique",
      e.getThematique(),
      "description",
      e.getDescription()
    );
  }

  public List<Map<String, Object>> teams() {
    return teams.findAll().stream().map(this::teamView).toList();
  }

  public Map<String, Object> teamDetail(Long id) {
    var v = teamView(team(id));
    v.put("membres", members.list(null, null, id, 0, 100, true).get("content"));
    return v;
  }

  public Map<String, Object> saveTeam(Long id, TeamRequest r) {
    user.director();
    text(r.nom(), 120);
    var e = id == null ? new Equipe() : team(id);
    e.setNom(r.nom());
    e.setThematique(r.thematique());
    e.setDescription(r.description());
    return teamView(teams.save(e));
  }

  public Map<String, Object> eventDetail(Long id) {
    return eventView(event(id));
  }

  public Map<String, Object> eventView(Evenement e) {
    return of(
      "id",
      e.getId(),
      "titre",
      e.getTitre(),
      "typeEvenement",
      e.getTypeEvenement(),
      "description",
      e.getDescription(),
      "edition",
      e.getEdition(),
      "dateDebut",
      e.getDateDebut(),
      "dateFin",
      e.getDateFin(),
      "lieu",
      e.getLieu(),
      "createurId",
      e.getCreePar().getId(),
      "organisateurIds",
      e.getOrganisateurs().stream().map(Membre::getId).sorted().toList()
    );
  }

  public List<Map<String, Object>> events(
    String q,
    String type,
    LocalDate debut,
    LocalDate fin
  ) {
    if (
      debut != null && fin != null && fin.isBefore(debut)
    ) throw new IllegalArgumentException("Intervalle invalide");
    return (
      q == null || q.isBlank()
        ? events.findAll()
        : events.findByTitreContainingIgnoreCase(q)
    )
      .stream()
      .filter(
        e -> q == null || e.getTitre().toLowerCase().contains(q.toLowerCase())
      )
      .filter(e -> type == null || e.getTypeEvenement().name().equals(type))
      .filter(
        e ->
          debut == null ||
          !(
            e.getDateFin() == null ? e.getDateDebut() : e.getDateFin()
          ).isBefore(debut)
      )
      .filter(e -> fin == null || !e.getDateDebut().isAfter(fin))
      .sorted(Comparator.comparing(Evenement::getDateDebut).reversed())
      .map(this::eventView)
      .toList();
  }

  public Map<String, Object> saveEvent(Long id, EventRequest r) {
    user.internal();
    text(r.titre(), 200);
    if (
      r.typeEvenement() == null ||
      r.dateDebut() == null ||
      (r.dateFin() != null && r.dateFin().isBefore(r.dateDebut()))
    ) throw new IllegalArgumentException("Type et dates cohérentes requis");
    var e = id == null ? new Evenement() : event(id);
    if (id == null) e.setCreePar(user.membre());
    else if (
      !e.getCreePar().getId().equals(user.membre().getId()) &&
      !user.has("DIRECTEUR")
    ) throw new org.springframework.security.access.AccessDeniedException(
      "Édition réservée au créateur"
    );
    e.setTitre(r.titre());
    e.setTypeEvenement(r.typeEvenement());
    e.setDescription(r.description());
    e.setEdition(r.edition());
    e.setDateDebut(r.dateDebut());
    e.setDateFin(r.dateFin());
    e.setLieu(r.lieu());
    if (r.organisateurIds() != null) {
      e.getOrganisateurs().clear();
      r.organisateurIds().forEach(i ->
        e.getOrganisateurs().add(members.require(i))
      );
    }
    events.saveAndFlush(e);
    if (id == null) publisher.publishEvent(
      new BusinessEvent(
        TypeNotification.EVENEMENT_CREE,
        "Nouvel événement",
        e.getTitre(),
        "/evenements",
        null,
        false
      )
    );
    return eventView(e);
  }

  public Map<String, Object> pubView(Publication p) {
    return of(
      "id",
      p.getId(),
      "titre",
      p.getTitre(),
      "typePublication",
      p.getTypePublication(),
      "annee",
      p.getAnnee(),
      "doi",
      p.getDoi(),
      "lien",
      p.getLien(),
      "equipeId",
      p.getEquipe() == null ? null : p.getEquipe().getId(),
      "equipe",
      p.getEquipe() == null ? null : p.getEquipe().getNom(),
      "createurId",
      p.getAjoutePar().getId(),
      "auteurs",
      p
        .getAuteurs()
        .stream()
        .sorted(Comparator.comparing(PublicationAuteur::getOrdreAuteur))
        .map(a ->
          of(
            "membreId",
            a.getMembre().getId(),
            "nom",
            a.getMembre().getPrenom() + " " + a.getMembre().getNom(),
            "ordre",
            a.getOrdreAuteur()
          )
        )
        .toList()
    );
  }

  public List<Map<String, Object>> publications(
    String q,
    Integer annee,
    String type,
    Long equipeId,
    Long auteurId
  ) {
    return (
      q == null || q.isBlank()
        ? pubs.findAll()
        : pubs.findByTitreContainingIgnoreCase(q)
    )
      .stream()
      .filter(
        p -> q == null || p.getTitre().toLowerCase().contains(q.toLowerCase())
      )
      .filter(p -> annee == null || annee.equals(p.getAnnee()))
      .filter(p -> type == null || type.equals(p.getTypePublication().name()))
      .filter(
        p ->
          equipeId == null ||
          (p.getEquipe() != null && equipeId.equals(p.getEquipe().getId()))
      )
      .filter(
        p ->
          auteurId == null ||
          p
            .getAuteurs()
            .stream()
            .anyMatch(a -> auteurId.equals(a.getMembre().getId()))
      )
      .sorted(Comparator.comparing(Publication::getAnnee).reversed())
      .map(this::pubView)
      .toList();
  }

  public Map<String, Object> publication(Long id) {
    return pubView(
      pubs
        .findById(id)
        .orElseThrow(() ->
          new EntityNotFoundException("Publication introuvable")
        )
    );
  }

  public Map<String, Object> savePublication(Long id, PubRequest r) {
    text(r.titre(), 300);
    if (
      r.typePublication() == null ||
      r.annee() == null ||
      r.annee() < 1900 ||
      r.annee() > LocalDate.now().getYear() + 1 ||
      r.auteurIds() == null ||
      r.auteurIds().isEmpty() ||
      new HashSet<>(r.auteurIds()).size() != r.auteurIds().size()
    ) throw new IllegalArgumentException(
      "Année, type et auteurs distincts requis"
    );
    Long me = user.membre().getId();
    if (
      user.has("ASSOCIE") && !r.auteurIds().contains(me)
    ) throw new org.springframework.security.access.AccessDeniedException(
      "Un associé doit être co-auteur"
    );
    if (
      !user.has("PERMANENT") && !user.has("DOCTORANT") && !user.has("ASSOCIE")
    ) throw new org.springframework.security.access.AccessDeniedException(
      "Statut scientifique requis"
    );
    var p =
      id == null
        ? new Publication()
        : pubs
            .findById(id)
            .orElseThrow(() ->
              new EntityNotFoundException("Publication introuvable")
            );
    if (id == null) p.setAjoutePar(user.membre());
    else if (
      !p.getAjoutePar().getId().equals(me) &&
      !user.has("DIRECTEUR") &&
      p
        .getAuteurs()
        .stream()
        .noneMatch(a -> a.getMembre().getId().equals(me))
    ) throw new org.springframework.security.access.AccessDeniedException(
      "Auteur requis"
    );
    p.setTitre(r.titre());
    p.setTypePublication(r.typePublication());
    p.setAnnee(r.annee());
    p.setDoi(r.doi());
    p.setLien(r.lien());
    p.setEquipe(r.equipeId() == null ? null : team(r.equipeId()));
    var desired = r.auteurIds();
    p.getAuteurs().removeIf(a -> !desired.contains(a.getMembre().getId()));
    for (int i = 0; i < desired.size(); i++) {
      Long mid = desired.get(i);
      var a = p
        .getAuteurs()
        .stream()
        .filter(x -> mid.equals(x.getMembre().getId()))
        .findFirst()
        .orElse(null);
      if (a == null) {
        a = new PublicationAuteur();
        a.setPublication(p);
        a.setMembre(members.require(mid));
        p.getAuteurs().add(a);
      }
      a.setOrdreAuteur(i + 1);
    }
    return pubView(pubs.saveAndFlush(p));
  }
}
