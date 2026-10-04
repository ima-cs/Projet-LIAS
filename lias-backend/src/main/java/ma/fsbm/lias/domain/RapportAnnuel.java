package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Rapport d'activité annuel agrégé (événements + publications + activités).
 * Un seul rapport par année (unicité). Le PDF généré est référencé via document.
 */
@Entity
@Table(name = "rapport_annuel",
       uniqueConstraints = @UniqueConstraint(name = "uk_rapport_annee", columnNames = "annee"))
@Getter @Setter @NoArgsConstructor
public class RapportAnnuel {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer annee;

    @Column(name = "date_generation", nullable = false)
    private LocalDateTime dateGeneration = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genere_par_membre_id", nullable = false)
    private Membre generePar;
}
