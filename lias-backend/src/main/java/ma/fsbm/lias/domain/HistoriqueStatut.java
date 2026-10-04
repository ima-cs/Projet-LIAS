package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.fsbm.lias.domain.enums.TypeStatut;

import java.time.LocalDate;

/**
 * Évolution du STATUT dans le temps. Période ouverte = statut courant.
 * Contrainte métier : au plus UN statut ouvert par membre (index partiel en base).
 */
@Entity
@Table(name = "historique_statut")
@Getter @Setter @NoArgsConstructor
public class HistoriqueStatut {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_statut", nullable = false, length = 20)
    private TypeStatut typeStatut;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;
}
