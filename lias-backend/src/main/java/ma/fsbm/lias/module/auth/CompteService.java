package ma.fsbm.lias.module.auth;

import jakarta.persistence.EntityNotFoundException;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.common.error.ConflictException;
import ma.fsbm.lias.domain.*;
import ma.fsbm.lias.domain.enums.*;
import ma.fsbm.lias.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompteService {

  private final org.springframework.context.ApplicationEventPublisher publisher;
  private final UtilisateurRepository users;
  private final PasswordEncoder passwords;
  private final ma.fsbm.lias.repository.MembreRepository members;
  private final jakarta.persistence.EntityManager em;
  private final org.springframework.core.env.Environment environment;

  @org.springframework.beans.factory.annotation.Value(
    "${lias.frontend-url:http://127.0.0.1:5173}"
  )
  private String frontendUrl;

  public Utilisateur inviter(String email) {
    var u = users
      .findByEmailIgnoreCase(email.trim())
      .orElseGet(Utilisateur::new);
    if (u.getId() != null) {
      em.lock(u, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
      if (
        members.findByUtilisateur_Id(u.getId()).isPresent() ||
        u.getMotDePasseHash() != null ||
        u.isEstAdmin()
      ) throw new ConflictException(
        "Cette adresse possède déjà un compte associé ou activé"
      );
    }
    u.setEmail(email.trim().toLowerCase());
    u.setEtatCompte(EtatCompte.ACTIF);
    u.setMotDePasseHash(null);
    u.setEstAdmin(false);
    String token;
    do {
      byte[] bytes = new byte[32];
      new java.security.SecureRandom().nextBytes(bytes);
      token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    } while (users.existsByTokenActivation(token));
    u.setTokenActivation(token);
    u.setTokenExpiration(LocalDateTime.now().plusHours(72));
    users.save(u);
    publisher.publishEvent(
      new InvitationCreated(u.getEmail(), u.getTokenActivation())
    );
    return u;
  }

  /** Private demo delivery: only the director-facing adhesion service calls this mapper. */
  public Map<String, Object> invitationDemo(Long utilisateurId) {
    if (!environment.matchesProfiles("demo")) return Map.of();
    var u = users.findById(utilisateurId).orElseThrow();
    if (
      u.getTokenActivation() == null ||
      u.getTokenExpiration() == null ||
      !u.getTokenExpiration().isAfter(LocalDateTime.now())
    ) return Map.of();
    return ma.fsbm.lias.common.Views.of(
      "activationUrl",
      frontendUrl.replaceAll("/+$", "") +
        "/activation?token=" +
        u.getTokenActivation(),
      "activationExpiration",
      u.getTokenExpiration()
    );
  }

  public void etat(Long id, EtatCompte etat) {
    users
      .findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Compte introuvable"))
      .setEtatCompte(etat);
  }

  public void activer(String token, String password) {
    if (
      token == null ||
      token.isBlank() ||
      password == null ||
      password.length() < 8 ||
      password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
    ) throw new IllegalArgumentException(
      "Mot de passe requis : 8 à 72 caractères"
    );
    var u = users
      .findByTokenActivation(token)
      .orElseThrow(() -> new IllegalArgumentException("Invitation invalide"));
    if (
      u.getEtatCompte() != EtatCompte.ACTIF ||
      u.getTokenExpiration() == null ||
      !u.getTokenExpiration().isAfter(LocalDateTime.now())
    ) throw new IllegalArgumentException("Invitation expirée");
    u.setMotDePasseHash(passwords.encode(password));
    u.setTokenActivation(null);
    u.setTokenExpiration(null);
  }
}
