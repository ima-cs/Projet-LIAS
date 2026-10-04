package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.fsbm.lias.domain.enums.EtatCompte;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

/**
 * Compte technique d'authentification. Relation 1-1 avec {@link Membre}.
 * L'état du compte (ACTIF/GELE/DESACTIVE) est l'axe « accès », indépendant du statut et du rôle.
 * @Audited : on trace l'évolution de l'état du compte.
 */
@Entity
@Table(name = "utilisateur",
       uniqueConstraints = @UniqueConstraint(name = "uk_utilisateur_email", columnNames = "email"))
@Audited
@Getter @Setter @NoArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String email;

    /** Hash BCrypt. NULL tant que l'invitation n'est pas activée. */
    @Column(name = "mot_de_passe_hash", length = 100)
    private String motDePasseHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_compte", nullable = false, length = 20)
    private EtatCompte etatCompte = EtatCompte.ACTIF;

    /** Marqueur technique de l'administrateur système (axe « accès technique »,
     *  indépendant du statut et du rôle de gouvernance). */
    @Column(name = "est_admin", nullable = false)
    private boolean estAdmin = false;

    /** Jeton d'invitation (définition du mot de passe). Effacé après activation. */
    @Column(name = "token_activation", length = 120)
    private String tokenActivation;

    @Column(name = "token_expiration")
    private LocalDateTime tokenExpiration;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;
}
