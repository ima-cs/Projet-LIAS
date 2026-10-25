package ma.fsbm.lias.common;

import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.domain.Membre;
import ma.fsbm.lias.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {

  private final UtilisateurRepository users;
  private final MembreRepository members;

  public boolean has(String role) {
    var a = SecurityContextHolder.getContext().getAuthentication();
    return (
      a != null &&
      a
        .getAuthorities()
        .stream()
        .anyMatch(x -> x.getAuthority().equals("ROLE_" + role))
    );
  }

  public Membre membre() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null) throw new AccessDeniedException("Authentification requise");
    return members
      .findByUtilisateur_Id(
        users
          .findByEmailIgnoreCase(a.getName())
          .orElseThrow(() -> new AccessDeniedException("Compte absent"))
          .getId()
      )
      .orElseThrow();
  }

  public void self(Long id) {
    if (
      !has("ADMIN") && !membre().getId().equals(id)
    ) throw new AccessDeniedException("Profil privé");
  }

  public void internal() {
    if (!has("PERMANENT")) throw new AccessDeniedException(
      "Réservé aux permanents"
    );
  }

  public void director() {
    if (!has("DIRECTEUR")) throw new AccessDeniedException(
      "Directeur actif requis"
    );
  }
}
