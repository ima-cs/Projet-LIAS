package ma.fsbm.lias.module.historique;

import static ma.fsbm.lias.common.Views.of;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.common.error.ConflictException;
import ma.fsbm.lias.domain.*;
import ma.fsbm.lias.domain.enums.*;
import ma.fsbm.lias.module.auth.CompteService;
import ma.fsbm.lias.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HistoriqueService {

  private final EntityManager em;
  private final MembreRepository membres;
  private final CompteService comptes;

  private Membre member(Long id) {
    var m = membres
      .findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Membre introuvable"));
    em.lock(m, LockModeType.PESSIMISTIC_WRITE);
    return m;
  }

  private void date(LocalDate d) {
    if (
      d == null || d.isAfter(LocalDate.now())
    ) throw new IllegalArgumentException("Date requise, sans anticipation");
  }

  private <T> List<T> rows(Class<T> c, Long id) {
    return em
      .createQuery(
        "from " +
          c.getSimpleName() +
          " x where x.membre.id=:id order by x.dateDebut",
        c
      )
      .setParameter("id", id)
      .getResultList();
  }

  private void bounds(LocalDate start, LocalDate end) {
    if (end.isBefore(start)) throw new IllegalArgumentException(
      "La date précède la période existante"
    );
  }

  public void initial(Long id, TypeStatut type, LocalDate d) {
    member(id);
    affiliation(id, "RETOUR", d, type);
  }

  public void statut(Long id, TypeStatut type, LocalDate d) {
    date(d);
    if (type == null) throw new IllegalArgumentException("Statut requis");
    var m = member(id);
    var all = rows(HistoriqueStatut.class, id);
    for (var h : all) {
      if (h.getDateFin() == null) {
        bounds(h.getDateDebut(), d);
        h.setDateFin(d);
      } else bounds(h.getDateFin(), d);
    }
    em.flush();
    var h = new HistoriqueStatut();
    h.setMembre(m);
    h.setTypeStatut(type);
    h.setDateDebut(d);
    em.persist(h);
    if (type == TypeStatut.RETRAITE || type == TypeStatut.ANCIEN) {
      closeAff(id, d);
      for (var r : rows(HistoriqueRole.class, id))
        if (r.getDateFin() == null) {
          bounds(r.getDateDebut(), d);
          r.setDateFin(d);
        }
      for (var e : rows(AppartenanceEquipe.class, id))
        if (e.getDateFin() == null) {
          bounds(e.getDateDebut(), d);
          e.setDateFin(d);
        }
      comptes.etat(
        m.getUtilisateur().getId(),
        type == TypeStatut.RETRAITE ? EtatCompte.GELE : EtatCompte.DESACTIVE
      );
    }
  }

  private void closeAff(Long id, LocalDate d) {
    for (var a : rows(Affiliation.class, id))
      if (a.getDateFin() == null) {
        bounds(a.getDateDebut(), d);
        a.setDateFin(d);
      }
  }

  public void affiliation(
    Long id,
    String action,
    LocalDate d,
    TypeStatut type
  ) {
    date(d);
    var m = member(id);
    if ("DEPART".equals(action)) {
      statut(id, TypeStatut.ANCIEN, d);
      return;
    }
    if (!"RETOUR".equals(action)) throw new IllegalArgumentException(
      "Action DEPART ou RETOUR requise"
    );
    for (var a : rows(Affiliation.class, id)) {
      if (a.getDateFin() == null) throw new ConflictException(
        "Une affiliation est déjà ouverte"
      );
      bounds(a.getDateFin(), d);
    }
    if (
      type == null || type == TypeStatut.ANCIEN || type == TypeStatut.RETRAITE
    ) throw new IllegalArgumentException("Statut actif requis pour le retour");
    var a = new Affiliation();
    a.setMembre(m);
    a.setDateDebut(d);
    em.persist(a);
    statut(id, type, d);
    comptes.etat(m.getUtilisateur().getId(), EtatCompte.ACTIF);
  }

  public void equipe(Long id, Long equipeId, LocalDate d) {
    date(d);
    var m = member(id);
    var e = em.find(Equipe.class, equipeId);
    if (e == null) throw new EntityNotFoundException("Équipe introuvable");
    for (var a : rows(AppartenanceEquipe.class, id)) {
      if (a.getDateFin() == null) {
        bounds(a.getDateDebut(), d);
        a.setDateFin(d);
      } else bounds(a.getDateFin(), d);
    }
    em.flush();
    var a = new AppartenanceEquipe();
    a.setMembre(m);
    a.setEquipe(e);
    a.setDateDebut(d);
    em.persist(a);
  }

  public Map<String, Object> role(
    Long id,
    TypeRole type,
    Long equipeId,
    Long mandatId,
    LocalDate d
  ) {
    date(d);
    var m = member(id);
    if (type == null) throw new IllegalArgumentException("Rôle requis");
    boolean team =
      type == TypeRole.CHEF_EQUIPE || type == TypeRole.MEMBRE_EQUIPE;
    if (team != (equipeId != null)) throw new IllegalArgumentException(
      "Équipe incompatible avec ce rôle"
    );
    Mandat mandat = mandatId == null ? null : em.find(Mandat.class, mandatId);
    if (type != TypeRole.MEMBRE_EQUIPE) {
      if (
        !"PERMANENT".equals(etat(id, d).get("statut")) ||
        mandat == null ||
        mandat.getDateFin() != null ||
        mandat.getDateDebut().isAfter(d)
      ) throw new ConflictException(
        "Gouvernance : permanent et mandat actif requis"
      );
    }
    Equipe e = equipeId == null ? null : em.find(Equipe.class, equipeId);
    if (team && e == null) throw new EntityNotFoundException(
      "Équipe introuvable"
    );
    var r = new HistoriqueRole();
    r.setMembre(m);
    r.setTypeRole(type);
    r.setEquipe(e);
    r.setMandat(mandat);
    r.setDateDebut(d);
    em.persist(r);
    em.flush();
    return of("id", r.getId(), "type", type, "dateDebut", d);
  }

  public void closeRole(Long id, Long roleId, LocalDate d) {
    date(d);
    member(id);
    var r = em.find(HistoriqueRole.class, roleId);
    if (
      r == null || !r.getMembre().getId().equals(id)
    ) throw new EntityNotFoundException("Rôle introuvable");
    if (r.getDateFin() != null) throw new ConflictException(
      "Rôle déjà clôturé"
    );
    bounds(r.getDateDebut(), d);
    r.setDateFin(d);
  }

  public Map<String, Object> mandat(String libelle, LocalDate d) {
    date(d);
    var m = new Mandat();
    m.setLibelle(libelle);
    m.setDateDebut(d);
    em.persist(m);
    em.flush();
    return of("id", m.getId(), "libelle", libelle, "dateDebut", d);
  }

  public void closeMandat(Long id, LocalDate d) {
    date(d);
    var m = em.find(Mandat.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (m == null) throw new EntityNotFoundException("Mandat introuvable");
    if (m.getDateFin() != null) throw new ConflictException(
      "Mandat déjà clôturé"
    );
    bounds(m.getDateDebut(), d);
    var roles = em
      .createQuery(
        "from HistoriqueRole r where r.mandat.id=:id and r.dateFin is null",
        HistoriqueRole.class
      )
      .setParameter("id", id)
      .getResultList();
    for (var r : roles) {
      bounds(r.getDateDebut(), d);
      r.setDateFin(d);
    }
    m.setDateFin(d);
  }

  private boolean active(LocalDate start, LocalDate end, LocalDate d) {
    return !start.isAfter(d) && (end == null || end.isAfter(d));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> etat(Long id, LocalDate d) {
    if (!membres.existsById(id)) throw new EntityNotFoundException(
      "Membre introuvable"
    );
    var statut = rows(HistoriqueStatut.class, id)
      .stream()
      .filter(h -> active(h.getDateDebut(), h.getDateFin(), d))
      .map(h -> h.getTypeStatut().name())
      .findFirst()
      .orElse(null);
    var equipe = rows(AppartenanceEquipe.class, id)
      .stream()
      .filter(h -> active(h.getDateDebut(), h.getDateFin(), d))
      .findFirst()
      .orElse(null);
    var roles = rows(HistoriqueRole.class, id)
      .stream()
      .filter(
        h ->
          active(h.getDateDebut(), h.getDateFin(), d) &&
          (h.getMandat() == null ||
            active(h.getMandat().getDateDebut(), h.getMandat().getDateFin(), d))
      )
      .map(h ->
        of(
          "id",
          h.getId(),
          "type",
          h.getTypeRole(),
          "equipeId",
          h.getEquipe() == null ? null : h.getEquipe().getId(),
          "mandatId",
          h.getMandat() == null ? null : h.getMandat().getId(),
          "dateDebut",
          h.getDateDebut()
        )
      )
      .toList();
    return of(
      "membreId",
      id,
      "date",
      d,
      "statut",
      statut,
      "equipeId",
      equipe == null ? null : equipe.getEquipe().getId(),
      "equipe",
      equipe == null ? null : equipe.getEquipe().getNom(),
      "roles",
      roles
    );
  }

  @Transactional(readOnly = true)
  public Map<String, Object> periodes(Long id) {
    return of(
      "statuts",
      rows(HistoriqueStatut.class, id)
        .stream()
        .map(h ->
          of(
            "type",
            h.getTypeStatut(),
            "dateDebut",
            h.getDateDebut(),
            "dateFin",
            h.getDateFin()
          )
        )
        .toList(),
      "affiliations",
      rows(Affiliation.class, id)
        .stream()
        .map(h -> of("dateDebut", h.getDateDebut(), "dateFin", h.getDateFin()))
        .toList(),
      "equipes",
      rows(AppartenanceEquipe.class, id)
        .stream()
        .map(h ->
          of(
            "equipe",
            h.getEquipe().getNom(),
            "dateDebut",
            h.getDateDebut(),
            "dateFin",
            h.getDateFin()
          )
        )
        .toList(),
      "roles",
      rows(HistoriqueRole.class, id)
        .stream()
        .map(h ->
          of(
            "id",
            h.getId(),
            "type",
            h.getTypeRole(),
            "dateDebut",
            h.getDateDebut(),
            "dateFin",
            h.getDateFin()
          )
        )
        .toList()
    );
  }
}
