package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Appartenance d'un membre à une équipe, historisée. Une seule équipe active à la fois :
 * au plus UNE période ouverte par membre (index partiel en base).
 * L'« équipe actuelle » d'un membre = appartenance ouverte.
 */
@Entity
@Table(name = "appartenance_equipe")
@Getter @Setter @NoArgsConstructor
public class AppartenanceEquipe {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipe_id", nullable = false)
    private Equipe equipe;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;
}
