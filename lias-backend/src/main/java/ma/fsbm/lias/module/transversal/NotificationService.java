package ma.fsbm.lias.module.transversal;

import static ma.fsbm.lias.common.Views.of;

import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.common.CurrentUser;
import ma.fsbm.lias.domain.*;
import ma.fsbm.lias.repository.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

  private final HistoriqueStatutRepository statuses;
  private final NotificationRepository repo;
  private final MembreRepository members;
  private final HistoriqueRoleRepository roles;
  private final CurrentUser user;

  @EventListener
  public void event(BusinessEvent e) {
    for (var m : members.findAll()) {
      if (
        java.util.Set.of(
          ma.fsbm.lias.domain.enums.TypeNotification.DOCUMENT_AJOUTE,
          ma.fsbm.lias.domain.enums.TypeNotification.RAPPORT_GENERE
        ).contains(e.type()) &&
        !statuses.findStatutCourant(m.getId()).orElse("").equals("PERMANENT")
      ) continue;
      if (
        e.destinataire() != null && !m.getId().equals(e.destinataire())
      ) continue;
      if (e.direction() && !roles.estDirecteurActif(m.getId())) continue;
      var n = new Notification();
      n.setDestinataire(m);
      n.setTypeNotification(e.type());
      n.setTitre(e.titre());
      n.setContenu(e.contenu());
      n.setLien(e.lien());
      repo.save(n);
    }
  }

  public List<Map<String, Object>> list() {
    return repo
      .findAll()
      .stream()
      .filter(n -> n.getDestinataire().getId().equals(user.membre().getId()))
      .sorted(Comparator.comparing(Notification::getDateCreation).reversed())
      .map(n ->
        of(
          "id",
          n.getId(),
          "titre",
          n.getTitre(),
          "contenu",
          n.getContenu(),
          "lien",
          n.getLien(),
          "type",
          n.getTypeNotification(),
          "lu",
          n.isLu(),
          "dateCreation",
          n.getDateCreation()
        )
      )
      .toList();
  }

  public void read(Long id) {
    var n = repo
      .findById(id)
      .orElseThrow(() ->
        new jakarta.persistence.EntityNotFoundException(
          "Notification introuvable"
        )
      );
    if (
      !n.getDestinataire().getId().equals(user.membre().getId())
    ) throw new org.springframework.security.access.AccessDeniedException(
      "Notification privée"
    );
    n.setLu(true);
  }
}
