package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.fsbm.lias.domain.enums.StatutConvention;

import java.time.LocalDate;

@Entity
@Table(name = "convention")
@Getter @Setter @NoArgsConstructor
public class Convention {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String titre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partenaire_id", nullable = false)
    private Partenaire partenaire;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_convention", nullable = false, length = 20)
    private StatutConvention statutConvention = StatutConvention.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;
}
