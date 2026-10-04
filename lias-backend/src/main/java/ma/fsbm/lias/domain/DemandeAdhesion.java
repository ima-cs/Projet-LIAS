package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.fsbm.lias.domain.enums.StatutDemande;

import java.time.LocalDateTime;

/**
 * Workflow d'adhésion. Acceptée -> création utilisateur + membre (membre_cree).
 * Refusée -> notification + motif. Traitée uniquement par le directeur en mandat.
 */
@Entity
@Table(name = "demande_adhesion")
@Getter @Setter @NoArgsConstructor
public class DemandeAdhesion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nom;

    @Column(nullable = false, length = 80)
    private String prenom;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(columnDefinition = "text")
    private String motivation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_document_id")
    private Document cv;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_demande", nullable = false, length = 20)
    private StatutDemande statutDemande = StatutDemande.EN_ATTENTE;

    @Column(name = "motif_refus", columnDefinition = "text")
    private String motifRefus;

    @Column(name = "date_soumission", nullable = false)
    private LocalDateTime dateSoumission = LocalDateTime.now();

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par_membre_id")
    private Membre traitePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membre_cree_id")
    private Membre membreCree;
}
