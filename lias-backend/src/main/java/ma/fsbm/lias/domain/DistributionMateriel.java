package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Trace « qui a reçu quoi ». L'équité est une vue de reporting, pas une allocation auto. */
@Entity
@Table(name = "distribution_materiel")
@Getter @Setter @NoArgsConstructor
public class DistributionMateriel {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materiel_id", nullable = false)
    private Materiel materiel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "date_distribution", nullable = false)
    private LocalDate dateDistribution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribue_par_membre_id", nullable = false)
    private Membre distribuePar;
}
