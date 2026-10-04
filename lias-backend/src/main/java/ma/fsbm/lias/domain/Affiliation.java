package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Période d'appartenance au laboratoire. Un membre peut quitter puis revenir :
 * chaque passage est une ligne. Période ouverte (dateFin == null) = affiliation active.
 * Contrainte métier : au plus UNE affiliation ouverte par membre (index partiel en base).
 */
@Entity
@Table(name = "affiliation")
@Getter @Setter @NoArgsConstructor
public class Affiliation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;
}
