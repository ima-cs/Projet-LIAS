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

  public Utilisateur inviter(String email) {
    if (
      users.findByEmailIgnoreCase(email.trim()).isPresent()
    ) throw new ConflictException("Cette adresse possède déjà un compte");
    var u = new Utilisateur();
    u.setEmail(email.trim().toLowerCase());
    u.setTokenActivation(UUID.randomUUID().toString());
    u.setTokenExpiration(LocalDateTime.now().plusHours(72));
    users.save(u);
    publisher.publishEvent(
      new InvitationCreated(u.getEmail(), u.getTokenActivation())
    );
    return u;
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
      password == null ||
      password.length() < 8 ||
      password.length() > 72
    ) throw new IllegalArgumentException(
      "Mot de passe requis : 8 à 72 caractères"
    );
    var u = users
      .findAll()
      .stream()
      .filter(x -> token.equals(x.getTokenActivation()))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Invitation invalide"));
    if (
      u.getTokenExpiration() == null ||
      !u.getTokenExpiration().isAfter(LocalDateTime.now())
    ) throw new IllegalArgumentException("Invitation expirée");
    u.setMotDePasseHash(passwords.encode(password));
    u.setTokenActivation(null);
    u.setTokenExpiration(null);
  }
}
