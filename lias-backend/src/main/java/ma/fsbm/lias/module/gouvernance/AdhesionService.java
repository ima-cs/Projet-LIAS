package ma.fsbm.lias.module.gouvernance;

import static ma.fsbm.lias.common.Views.of;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.common.CurrentUser;
import ma.fsbm.lias.domain.*;
import ma.fsbm.lias.domain.enums.*;
import ma.fsbm.lias.module.auth.CompteService;
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
public class AdhesionService {

  private final ma.fsbm.lias.module.science.DocumentService documents;
  private final DemandeAdhesionRepository repo;
  private final CompteService accounts;
  private final MembreService members;
  private final HistoriqueService history;
  private final CurrentUser user;
  private final ApplicationEventPublisher publisher;
  private final EntityManager em;

  public record Request(
    String nom,
    String prenom,
    String email,
    String motivation
  ) {}

  public record Decision(
    boolean accepter,
    String motif,
    TypeStatut statut,
    Long equipeId
  ) {}

  public Map<String, Object> view(DemandeAdhesion d) {
    var result = of(
      "id",
      d.getId(),
      "nom",
      d.getNom(),
      "prenom",
      d.getPrenom(),
      "email",
      d.getEmail(),
      "motivation",
      d.getMotivation(),
      "statutDemande",
      d.getStatutDemande(),
      "motifRefus",
      d.getMotifRefus(),
      "dateSoumission",
      d.getDateSoumission(),
      "dateDecision",
      d.getDateDecision(),
      "cvDocumentId",
      d.getCv() == null ? null : d.getCv().getId(),
      "membreCreeId",
      d.getMembreCree() == null ? null : d.getMembreCree().getId()
    );
    if (
      d.getStatutDemande() == StatutDemande.ACCEPTEE &&
      d.getMembreCree() != null
    ) result.putAll(
      accounts.invitationDemo(d.getMembreCree().getUtilisateur().getId())
    );
    return result;
  }

  public Map<String, Object> submit(Request r) {
    if (
      r.nom() == null ||
      r.nom().isBlank() ||
      r.nom().length() > 80 ||
      r.prenom() == null ||
      r.prenom().isBlank() ||
      r.prenom().length() > 80 ||
      r.email() == null ||
      r.email().length() > 180 ||
      !r.email().matches("^[^@\s]+@[^@\s]+[.][^@\s]+$")
    ) throw new IllegalArgumentException(
      "Nom, prénom et e-mail valides requis"
    );
    if (
      repo
        .findAll()
        .stream()
        .anyMatch(
          d ->
            d.getEmail().equalsIgnoreCase(r.email().trim()) &&
            d.getStatutDemande() == StatutDemande.EN_ATTENTE
        )
    ) throw new ma.fsbm.lias.common.error.ConflictException(
      "Une demande est déjà en attente pour cette adresse"
    );
    var d = new DemandeAdhesion();
    d.setNom(r.nom().trim());
    d.setPrenom(r.prenom().trim());
    d.setEmail(r.email().trim().toLowerCase());
    d.setMotivation(r.motivation());
    repo.saveAndFlush(d);
    publisher.publishEvent(
      new BusinessEvent(
        TypeNotification.ADHESION_SOUMISE,
        "Demande d'adhésion",
        d.getPrenom() + " " + d.getNom(),
        "/adhesions",
        null,
        true
      )
    );
    return of(
      "id",
      d.getId(),
      "statutDemande",
      d.getStatutDemande(),
      "message",
      "Votre demande a été enregistrée."
    );
  }

  public Map<String, Object> submitWithCv(
    Request r,
    org.springframework.web.multipart.MultipartFile file
  ) {
    var result = submit(r);
    if (file != null && !file.isEmpty()) repo
      .findById((Long) result.get("id"))
      .orElseThrow()
      .setCv(documents.publicCv(file));
    return result;
  }

  public List<Map<String, Object>> list() {
    user.director();
    return repo
      .findAll()
      .stream()
      .sorted(
        Comparator.comparing(DemandeAdhesion::getDateSoumission).reversed()
      )
      .map(this::view)
      .toList();
  }

  public Map<String, Object> decision(Long id, Decision r) {
    user.director();
    var d = repo
      .findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
    em.lock(d, LockModeType.PESSIMISTIC_WRITE);
    if (
      d.getStatutDemande() != StatutDemande.EN_ATTENTE
    ) throw new ma.fsbm.lias.common.error.ConflictException(
      "Demande déjà traitée"
    );
    if (r.accepter()) {
      var type = r.statut() == null ? TypeStatut.DOCTORANT : r.statut();
      if (
        type == TypeStatut.RETRAITE || type == TypeStatut.ANCIEN
      ) throw new IllegalArgumentException("Statut initial actif requis");
      var u = accounts.inviter(d.getEmail());
      var m = members.creer(u, d.getNom(), d.getPrenom());
      history.initial(m.getId(), type, LocalDate.now());
      if (r.equipeId() != null) history.equipe(
        m.getId(),
        r.equipeId(),
        LocalDate.now()
      );
      d.setMembreCree(m);
      d.setStatutDemande(StatutDemande.ACCEPTEE);
      publisher.publishEvent(
        new BusinessEvent(
          TypeNotification.ADHESION_DECISION,
          "Adhésion acceptée",
          "Bienvenue au LIAS",
          "/profil",
          m.getId(),
          false
        )
      );
    } else {
      if (
        r.motif() == null || r.motif().isBlank()
      ) throw new IllegalArgumentException("Motif de refus requis");
      d.setStatutDemande(StatutDemande.REFUSEE);
      d.setMotifRefus(r.motif());
      publisher.publishEvent(
        new BusinessEvent(
          TypeNotification.ADHESION_DECISION,
          "Adhésion refusée",
          d.getEmail() + " : " + r.motif(),
          "/adhesions",
          user.membre().getId(),
          false
        )
      );
    }
    d.setDateDecision(LocalDateTime.now());
    d.setTraitePar(user.membre());
    return view(d);
  }
}
