package ma.fsbm.lias.module.materiel;

import static ma.fsbm.lias.common.Views.of;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.common.CurrentUser;
import ma.fsbm.lias.domain.*;
import ma.fsbm.lias.domain.enums.*;
import ma.fsbm.lias.module.membre.MembreService;
import ma.fsbm.lias.module.transversal.BusinessEvent;
import ma.fsbm.lias.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterielService {

  private final MaterielRepository stock;
  private final BesoinMaterielRepository needs;
  private final DistributionMaterielRepository distributions;
  private final MembreService members;
  private final CurrentUser user;
  private final EntityManager em;
  private final ApplicationEventPublisher publisher;

  public record Arrivage(
    String designation,
    String description,
    Integer quantiteInitiale,
    LocalDate dateArrivage
  ) {}

  public record Besoin(
    String designation,
    String justification,
    Integer quantiteDemandee
  ) {}

  public record Distribution(
    Long materielId,
    Long membreId,
    Integer quantite,
    LocalDate dateDistribution
  ) {}

  public Map<String, Object> stockView(Materiel m) {
    return of(
      "id",
      m.getId(),
      "designation",
      m.getDesignation(),
      "description",
      m.getDescription(),
      "quantiteInitiale",
      m.getQuantiteInitiale(),
      "quantiteDisponible",
      m.getQuantiteDisponible(),
      "dateArrivage",
      m.getDateArrivage()
    );
  }

  private void valid(String text, Integer q) {
    if (
      text == null ||
      text.isBlank() ||
      text.length() > 200 ||
      q == null ||
      q < 1
    ) throw new IllegalArgumentException(
      "Désignation et quantité positive requises"
    );
  }

  public List<Map<String, Object>> stock() {
    user.internal();
    return stock.findAll().stream().map(this::stockView).toList();
  }

  public Map<String, Object> arrive(Arrivage r) {
    user.director();
    valid(r.designation(), r.quantiteInitiale());
    if (r.dateArrivage() == null) throw new IllegalArgumentException(
      "Date requise"
    );
    var m = new Materiel();
    m.setDesignation(r.designation());
    m.setDescription(r.description());
    m.setQuantiteInitiale(r.quantiteInitiale());
    m.setQuantiteDisponible(r.quantiteInitiale());
    m.setDateArrivage(r.dateArrivage());
    return stockView(stock.save(m));
  }

  public Map<String, Object> needView(BesoinMateriel b) {
    return of(
      "id",
      b.getId(),
      "designation",
      b.getDesignation(),
      "justification",
      b.getJustification(),
      "quantiteDemandee",
      b.getQuantiteDemandee(),
      "statutBesoin",
      b.getStatutBesoin(),
      "demandeurId",
      b.getDemandeur().getId(),
      "demandeur",
      b.getDemandeur().getPrenom() + " " + b.getDemandeur().getNom(),
      "dateDemande",
      b.getDateDemande(),
      "dateDecision",
      b.getDateDecision()
    );
  }

  public List<Map<String, Object>> needs() {
    user.internal();
    return needs
      .findAll()
      .stream()
      .filter(
        b ->
          user.has("DIRECTEUR") ||
          b.getDemandeur().getId().equals(user.membre().getId())
      )
      .map(this::needView)
      .toList();
  }

  public Map<String, Object> need(Besoin r) {
    user.internal();
    valid(r.designation(), r.quantiteDemandee());
    var b = new BesoinMateriel();
    b.setDesignation(r.designation());
    b.setJustification(r.justification());
    b.setQuantiteDemandee(r.quantiteDemandee());
    b.setDemandeur(user.membre());
    return needView(needs.save(b));
  }

  public Map<String, Object> decide(Long id, StatutBesoin decision) {
    user.director();
    if (
      decision == null || decision == StatutBesoin.EN_ATTENTE
    ) throw new IllegalArgumentException("Décision VALIDE ou REFUSE requise");
    var b = needs
      .findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Besoin introuvable"));
    em.lock(b, LockModeType.PESSIMISTIC_WRITE);
    if (
      b.getStatutBesoin() != StatutBesoin.EN_ATTENTE
    ) throw new ma.fsbm.lias.common.error.ConflictException(
      "Besoin déjà traité"
    );
    b.setStatutBesoin(decision);
    b.setTraitePar(user.membre());
    b.setDateDecision(LocalDateTime.now());
    publisher.publishEvent(
      new BusinessEvent(
        TypeNotification.BESOIN_DECISION,
        "Décision matériel",
        b.getDesignation() + " : " + decision,
        "/besoins",
        b.getDemandeur().getId(),
        false
      )
    );
    return needView(b);
  }

  public Map<String, Object> distribute(Distribution r) {
    user.director();
    if (
      r.quantite() == null || r.quantite() < 1 || r.dateDistribution() == null
    ) throw new IllegalArgumentException("Quantité et date requises");
    var m = em.find(
      Materiel.class,
      r.materielId(),
      LockModeType.PESSIMISTIC_WRITE
    );
    if (m == null) throw new EntityNotFoundException("Matériel introuvable");
    if (
      m.getQuantiteDisponible() < r.quantite()
    ) throw new ma.fsbm.lias.common.error.ConflictException(
      "Stock disponible insuffisant"
    );
    var beneficiary = members.require(r.membreId());
    m.setQuantiteDisponible(m.getQuantiteDisponible() - r.quantite());
    var d = new DistributionMateriel();
    d.setMateriel(m);
    d.setMembre(beneficiary);
    d.setQuantite(r.quantite());
    d.setDateDistribution(r.dateDistribution());
    d.setDistribuePar(user.membre());
    distributions.save(d);
    return of("id", d.getId(), "quantiteDisponible", m.getQuantiteDisponible());
  }

  public List<Map<String, Object>> equity() {
    user.director();
    var all = distributions.findAll();
    var ms = (List<Map<String, Object>>) members
      .list(null, "PERMANENT", null, 0, 100, true)
      .get("content");
    return ms
      .stream()
      .map(m ->
        of(
          "membreId",
          m.get("id"),
          "membre",
          m.get("prenom") + " " + m.get("nom"),
          "quantiteTotale",
          all
            .stream()
            .filter(d -> d.getMembre().getId().equals(m.get("id")))
            .mapToInt(DistributionMateriel::getQuantite)
            .sum(),
          "distributions",
          all
            .stream()
            .filter(d -> d.getMembre().getId().equals(m.get("id")))
            .map(d ->
              of(
                "id",
                d.getId(),
                "materiel",
                d.getMateriel().getDesignation(),
                "quantite",
                d.getQuantite(),
                "date",
                d.getDateDistribution()
              )
            )
            .toList()
        )
      )
      .toList();
  }
}
