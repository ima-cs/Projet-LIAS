package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "materiel")
@Getter @Setter @NoArgsConstructor
public class Materiel {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String designation;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "quantite_initiale", nullable = false)
    private Integer quantiteInitiale;

    @Column(name = "quantite_disponible", nullable = false)
    private Integer quantiteDisponible;

    @Column(name = "date_arrivage", nullable = false)
    private LocalDate dateArrivage;
}
