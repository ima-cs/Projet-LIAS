package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Profil métier d'un membre. @Audited : toute modification du profil est historisée
 * automatiquement par Hibernate Envers (table membre_aud), conformément au besoin
 * « historisation automatique des changements ».
 *
 * Le STATUT courant, le RÔLE courant et l'ÉQUIPE courante ne sont PAS des colonnes ici :
 * ils sont dérivés des entités d'historique (période ouverte = date_fin IS NULL).
 */
@Entity
@Table(name = "membre")
@Audited
@Getter @Setter @NoArgsConstructor
public class Membre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(nullable = false, length = 80)
    private String nom;

    @Column(nullable = false, length = 80)
    private String prenom;

    @Column(name = "photo_chemin", length = 255)
    private String photoChemin;

    @Column(columnDefinition = "text")
    private String biographie;

    /** Confidentielle : jamais exposée dans les réponses publiques. */
    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "date_embauche")
    private LocalDate dateEmbauche;

    @Column(name = "etablissement_origine", length = 160)
    private String etablissementOrigine;

    @Column(name = "laboratoire_origine", length = 160)
    private String laboratoireOrigine;

    /** Centres d'intérêt : collection de libellés simples. */
    @ElementCollection
    @CollectionTable(name = "membre_centre_interet",
            joinColumns = @JoinColumn(name = "membre_id"))
    @Column(name = "centre_interet", length = 120)
    private Set<String> centresInteret = new HashSet<>();
}
