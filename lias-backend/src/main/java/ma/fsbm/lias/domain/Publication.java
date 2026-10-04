package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.fsbm.lias.domain.enums.TypePublication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "publication")
@Getter @Setter @NoArgsConstructor
public class Publication {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_publication", nullable = false, length = 30)
    private TypePublication typePublication;

    @Column(nullable = false)
    private Integer annee;

    @Column(length = 120)
    private String doi;

    @Column(length = 300)
    private String lien;

    /** Équipe de rattachement (optionnelle), pour le classement par équipe. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id")
    private Equipe equipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ajoute_par_membre_id", nullable = false)
    private Membre ajoutePar;

    @Column(name = "date_ajout", nullable = false)
    private LocalDateTime dateAjout = LocalDateTime.now();

    @OneToMany(mappedBy = "publication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PublicationAuteur> auteurs = new ArrayList<>();
}
