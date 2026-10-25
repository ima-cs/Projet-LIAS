package ma.fsbm.lias.module.auth;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ma.fsbm.lias.config.security.JwtService;
import ma.fsbm.lias.domain.Membre;
import ma.fsbm.lias.domain.Utilisateur;
import ma.fsbm.lias.domain.enums.EtatCompte;
import ma.fsbm.lias.module.auth.dto.LoginRequest;
import ma.fsbm.lias.module.auth.dto.LoginResponse;
import ma.fsbm.lias.module.auth.dto.MeResponse;
import ma.fsbm.lias.module.auth.dto.ProfilAuthDto;
import ma.fsbm.lias.repository.HistoriqueRoleRepository;
import ma.fsbm.lias.repository.HistoriqueStatutRepository;
import ma.fsbm.lias.repository.MembreRepository;
import ma.fsbm.lias.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

  private final UtilisateurRepository utilisateurRepository;
  private final MembreRepository membreRepository;
  private final HistoriqueStatutRepository statutRepository;
  private final HistoriqueRoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
    UtilisateurRepository utilisateurRepository,
    MembreRepository membreRepository,
    HistoriqueStatutRepository statutRepository,
    HistoriqueRoleRepository roleRepository,
    PasswordEncoder passwordEncoder,
    JwtService jwtService
  ) {
    this.utilisateurRepository = utilisateurRepository;
    this.membreRepository = membreRepository;
    this.statutRepository = statutRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public LoginResponse login(LoginRequest request) {
    if (
      request == null ||
      request.email() == null ||
      request.email().isBlank() ||
      request.motDePasse() == null ||
      request.motDePasse().isBlank()
    ) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Email et mot de passe obligatoires."
      );
    }

    Utilisateur utilisateur = utilisateurRepository
      .findByEmailIgnoreCase(request.email().trim())
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "Identifiants invalides."
        )
      );

    if (utilisateur.getEtatCompte() == EtatCompte.GELE) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte gelé.");
    }

    if (utilisateur.getEtatCompte() == EtatCompte.DESACTIVE) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "Compte désactivé."
      );
    }

    if (
      utilisateur.getMotDePasseHash() == null ||
      !passwordEncoder.matches(
        request.motDePasse(),
        utilisateur.getMotDePasseHash()
      )
    ) {
      throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED,
        "Identifiants invalides."
      );
    }

    Membre membre = membreRepository
      .findByUtilisateur_Id(utilisateur.getId())
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "Identifiants invalides."
        )
      );

    List<String> autorites = calculerAutorites(utilisateur, membre.getId());

    utilisateur.setDerniereConnexion(LocalDateTime.now());
    utilisateurRepository.save(utilisateur);

    String token = jwtService.generateAccessToken(
      utilisateur.getEmail(),
      membre.getId(),
      autorites
    );

    return new LoginResponse(token, 1800, profil(utilisateur, membre));
  }

  @Transactional(readOnly = true)
  public MeResponse me(String email) {
    var account = utilisateurRepository
      .findByEmailIgnoreCase(email)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if (
      account.getEtatCompte() != EtatCompte.ACTIF
    ) throw new ResponseStatusException(
      HttpStatus.UNAUTHORIZED,
      "Compte indisponible"
    );

    Utilisateur utilisateur = utilisateurRepository
      .findByEmailIgnoreCase(email)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "Utilisateur non authentifié."
        )
      );

    Membre membre = membreRepository
      .findByUtilisateur_Id(utilisateur.getId())
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Profil membre introuvable."
        )
      );

    return new MeResponse(
      profil(utilisateur, membre),
      calculerAutorites(utilisateur, membre.getId())
    );
  }

  private List<String> calculerAutorites(
    Utilisateur utilisateur,
    Long membreId
  ) {
    List<String> autorites = new ArrayList<>();

    statutRepository.findStatutCourant(membreId).ifPresent(statut -> {
      switch (statut) {
        case "PERMANENT" -> autorites.add("ROLE_PERMANENT");
        case "ASSOCIE" -> autorites.add("ROLE_ASSOCIE");
        case "DOCTORANT" -> autorites.add("ROLE_DOCTORANT");
      }
    });

    if (
      autorites.contains("ROLE_PERMANENT") &&
      roleRepository.estDirecteurActif(membreId)
    ) {
      autorites.add("ROLE_DIRECTEUR");
    }

    if (utilisateur.isEstAdmin()) {
      autorites.add("ROLE_ADMIN");
    }

    return autorites;
  }

  private ProfilAuthDto profil(Utilisateur utilisateur, Membre membre) {
    return new ProfilAuthDto(
      membre.getId(),
      utilisateur.getEmail(),
      membre.getNom(),
      membre.getPrenom()
    );
  }
}
