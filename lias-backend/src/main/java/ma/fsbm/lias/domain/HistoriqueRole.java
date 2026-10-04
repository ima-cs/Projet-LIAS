package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.fsbm.lias.domain.enums.TypeRole;

import java.time.LocalDate;

/**
 * Évolution des RÔLES / responsabilités, lié à un mandat de gouvernance.
 * equipe_id : renseigné pour CHEF_EQUIPE / MEMBRE_EQUIPE ; null pour DIRECTEUR / VICE_DIRECTEUR.
 * mandat_id : renseigné pour les rôles de gouvernance (directeur, vice, chefs).
 * Période ouverte = rôle courant. Un membre peut cumuler plusieurs rôles ouverts
 * (ex. MEMBRE_EQUIPE + CHEF_EQUIPE) : pas de contrainte d'unicité sur la période ouverte.
 */
@Entity
@Table(name = "historique_role")
@Getter @Setter @NoArgsConstructor
public class HistoriqueRole {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_role", nullable = false, length = 20)
    private TypeRole typeRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id")
    private Equipe equipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mandat_id")
    private Mandat mandat;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;
}
