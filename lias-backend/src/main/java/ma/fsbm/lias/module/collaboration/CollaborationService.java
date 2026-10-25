package ma.fsbm.lias.module.collaboration;

import static ma.fsbm.lias.common.Views.of;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.common.CurrentUser;
import ma.fsbm.lias.domain.*;
import ma.fsbm.lias.domain.enums.*;
import ma.fsbm.lias.module.historique.HistoriqueService;
import ma.fsbm.lias.module.science.*;
import ma.fsbm.lias.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CollaborationService {

  private final PartenaireRepository partners;
  private final ConventionRepository agreements;
  private final DiscussionRepository threads;
  private final MessageRepository messages;
  private final CurrentUser user;
  private final DocumentService documents;
  private final ScienceService science;
  private final HistoriqueService history;

  public record Partner(
    String nom,
    TypePartenaire typePartenaire,
    String contactEmail,
    String pays,
    String description
  ) {}

  public record Agreement(
    String titre,
    Long partenaireId,
    LocalDate dateDebut,
    LocalDate dateFin,
    StatutConvention statutConvention,
    Long documentId
  ) {}

  public record Thread(
    String titre,
    TypeDiscussion typeDiscussion,
    Long equipeId,
    Long evenementId
  ) {}

  private void text(String s, int n) {
    if (
      s == null || s.isBlank() || s.length() > n
    ) throw new IllegalArgumentException(
      "Titre ou nom requis, longueur invalide"
    );
  }

  public List<Map<String, Object>> partners() {
    user.internal();
    return partners.findAll().stream().map(this::partnerView).toList();
  }

  public Map<String, Object> partnerView(Partenaire p) {
    return of(
      "id",
      p.getId(),
      "nom",
      p.getNom(),
      "typePartenaire",
      p.getTypePartenaire(),
      "contactEmail",
      p.getContactEmail(),
      "pays",
      p.getPays(),
      "description",
      p.getDescription()
    );
  }

  public Map<String, Object> partner(Long id, Partner r) {
    user.director();
    text(r.nom(), 200);
    if (r.typePartenaire() == null) throw new IllegalArgumentException(
      "Type requis"
    );
    var p =
      id == null
        ? new Partenaire()
        : partners
            .findById(id)
            .orElseThrow(() ->
              new EntityNotFoundException("Partenaire introuvable")
            );
    p.setNom(r.nom());
    p.setTypePartenaire(r.typePartenaire());
    p.setContactEmail(r.contactEmail());
    p.setPays(r.pays());
    p.setDescription(r.description());
    return partnerView(partners.save(p));
  }

  public Map<String, Object> agreementView(Convention c) {
    return of(
      "id",
      c.getId(),
      "titre",
      c.getTitre(),
      "partenaireId",
      c.getPartenaire().getId(),
      "partenaire",
      c.getPartenaire().getNom(),
      "dateDebut",
      c.getDateDebut(),
      "dateFin",
      c.getDateFin(),
      "statutConvention",
      c.getStatutConvention(),
      "documentId",
      c.getDocument() == null ? null : c.getDocument().getId()
    );
  }

  public List<Map<String, Object>> agreements() {
    user.internal();
    return agreements.findAll().stream().map(this::agreementView).toList();
  }

  public Map<String, Object> agreement(Long id, Agreement r) {
    user.director();
    text(r.titre(), 250);
    if (
      r.dateDebut() == null ||
      (r.dateFin() != null && r.dateFin().isBefore(r.dateDebut()))
    ) throw new IllegalArgumentException("Dates invalides");
    var c =
      id == null
        ? new Convention()
        : agreements
            .findById(id)
            .orElseThrow(() ->
              new EntityNotFoundException("Convention introuvable")
            );
    c.setTitre(r.titre());
    c.setPartenaire(
      partners
        .findById(r.partenaireId())
        .orElseThrow(() ->
          new EntityNotFoundException("Partenaire introuvable")
        )
    );
    c.setDateDebut(r.dateDebut());
    c.setDateFin(r.dateFin());
    c.setStatutConvention(
      r.statutConvention() == null
        ? StatutConvention.ACTIVE
        : r.statutConvention()
    );
    c.setDocument(
      r.documentId() == null ? null : documents.require(r.documentId())
    );
    return agreementView(agreements.save(c));
  }

  private boolean accessible(Discussion d) {
    if (d.getTypeDiscussion() == TypeDiscussion.EQUIPE) return (
      d.getEquipe() != null &&
      d
        .getEquipe()
        .getId()
        .equals(
          history.etat(user.membre().getId(), LocalDate.now()).get("equipeId")
        )
    );
    if (d.getTypeDiscussion() == TypeDiscussion.EVENEMENT) return (
      d.getEvenement() != null &&
      (user.has("PERMANENT") ||
        d
          .getEvenement()
          .getOrganisateurs()
          .stream()
          .anyMatch(m -> m.getId().equals(user.membre().getId())))
    );
    return (
      user.has("PERMANENT") || user.has("DOCTORANT") || user.has("ASSOCIE")
    );
  }

  public Map<String, Object> threadView(Discussion d) {
    return of(
      "id",
      d.getId(),
      "titre",
      d.getTitre(),
      "typeDiscussion",
      d.getTypeDiscussion(),
      "equipeId",
      d.getEquipe() == null ? null : d.getEquipe().getId(),
      "evenementId",
      d.getEvenement() == null ? null : d.getEvenement().getId(),
      "dateCreation",
      d.getDateCreation()
    );
  }

  public List<Map<String, Object>> threads() {
    return threads
      .findAll()
      .stream()
      .filter(this::accessible)
      .map(this::threadView)
      .toList();
  }

  public Map<String, Object> thread(Thread r) {
    text(r.titre(), 200);
    if (r.typeDiscussion() == null) throw new IllegalArgumentException(
      "Type requis"
    );
    if (
      (r.typeDiscussion() == TypeDiscussion.EQUIPE) != (r.equipeId() != null) ||
      (r.typeDiscussion() == TypeDiscussion.EVENEMENT) !=
        (r.evenementId() != null)
    ) throw new IllegalArgumentException("Contexte de discussion incohérent");
    var d = new Discussion();
    d.setTitre(r.titre());
    d.setTypeDiscussion(r.typeDiscussion());
    d.setCreePar(user.membre());
    d.setEquipe(r.equipeId() == null ? null : science.team(r.equipeId()));
    d.setEvenement(
      r.evenementId() == null ? null : science.event(r.evenementId())
    );
    if (
      !accessible(d)
    ) throw new org.springframework.security.access.AccessDeniedException(
      "Contexte non accessible"
    );
    return threadView(threads.save(d));
  }

  private Discussion access(Long id) {
    var d = threads
      .findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Discussion introuvable"));
    if (
      !accessible(d)
    ) throw new org.springframework.security.access.AccessDeniedException(
      "Discussion privée"
    );
    return d;
  }

  public List<Map<String, Object>> messages(Long id) {
    access(id);
    return messages
      .findAll()
      .stream()
      .filter(m -> m.getDiscussion().getId().equals(id))
      .sorted(Comparator.comparing(Message::getDateEnvoi))
      .map(m ->
        of(
          "id",
          m.getId(),
          "contenu",
          m.getContenu(),
          "auteurId",
          m.getAuteur().getId(),
          "auteur",
          m.getAuteur().getPrenom() + " " + m.getAuteur().getNom(),
          "dateEnvoi",
          m.getDateEnvoi()
        )
      )
      .toList();
  }

  public Map<String, Object> message(Long id, String text) {
    var d = access(id);
    text(text, 10000);
    var m = new Message();
    m.setDiscussion(d);
    m.setAuteur(user.membre());
    m.setContenu(text);
    messages.save(m);
    return of("id", m.getId(), "contenu", m.getContenu());
  }
}
