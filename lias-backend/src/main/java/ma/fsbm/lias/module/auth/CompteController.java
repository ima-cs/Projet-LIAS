package ma.fsbm.lias.module.auth;

import static ma.fsbm.lias.common.Views.of;

import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.domain.enums.EtatCompte;
import ma.fsbm.lias.repository.UtilisateurRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CompteController {

  private final CompteService service;
  private final UtilisateurRepository users;

  public record Activation(String token, String motDePasse) {}

  public record Etat(EtatCompte etat) {}

  @PostMapping("/api/auth/activer")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void activate(@RequestBody Activation r) {
    service.activer(r.token(), r.motDePasse());
  }

  @GetMapping("/api/admin/comptes")
  @PreAuthorize("hasRole('ADMIN')")
  public List<Map<String, Object>> list() {
    return users
      .findAll()
      .stream()
      .map(u ->
        of(
          "id",
          u.getId(),
          "email",
          u.getEmail(),
          "etatCompte",
          u.getEtatCompte(),
          "estAdmin",
          u.isEstAdmin(),
          "derniereConnexion",
          u.getDerniereConnexion()
        )
      )
      .toList();
  }

  @PatchMapping("/api/admin/comptes/{id}/etat")
  @PreAuthorize("hasRole('ADMIN')")
  public Map<String, Object> state(@PathVariable Long id, @RequestBody Etat r) {
    if (r.etat() == null) throw new IllegalArgumentException("État requis");
    service.etat(id, r.etat());
    return of("id", id, "etatCompte", r.etat());
  }
}
