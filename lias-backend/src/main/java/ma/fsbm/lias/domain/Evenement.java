package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.fsbm.lias.domain.enums.TypeEvenement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "evenement")
@Getter @Setter @NoArgsConstructor
public class Evenement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_evenement", nullable = false, length = 20)
    private TypeEvenement typeEvenement;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "text")
    private String description;

    /** Numéro / libellé d'édition pour l'archivage des éditions successives. */
    @Column(length = 60)
    private String edition;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(length = 200)
    private String lieu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cree_par_membre_id", nullable = false)
    private Membre creePar;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    /** Organisateurs : association n-n simple (sans attribut). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "evenement_organisateur",
        joinColumns = @JoinColumn(name = "evenement_id"),
        inverseJoinColumns = @JoinColumn(name = "membre_id"))
    private Set<Membre> organisateurs = new HashSet<>();
}
