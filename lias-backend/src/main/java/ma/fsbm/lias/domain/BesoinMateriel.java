package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.fsbm.lias.domain.enums.StatutBesoin;

import java.time.LocalDateTime;

@Entity
@Table(name = "besoin_materiel")
@Getter @Setter @NoArgsConstructor
public class BesoinMateriel {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demandeur_membre_id", nullable = false)
    private Membre demandeur;

    @Column(nullable = false, length = 200)
    private String designation;

    @Column(columnDefinition = "text")
    private String justification;

    @Column(name = "quantite_demandee", nullable = false)
    private Integer quantiteDemandee;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_besoin", nullable = false, length = 20)
    private StatutBesoin statutBesoin = StatutBesoin.EN_ATTENTE;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande = LocalDateTime.now();

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par_membre_id")
    private Membre traitePar;
}
