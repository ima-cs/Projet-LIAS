package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "reunion")
@Getter @Setter @NoArgsConstructor
public class Reunion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(name = "date_reunion", nullable = false)
    private LocalDate dateReunion;

    @Column(name = "ordre_du_jour", columnDefinition = "text")
    private String ordreDuJour;

    /** PV déposé (document de type PV), renseigné après la réunion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pv_document_id")
    private Document pvDocument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cree_par_membre_id", nullable = false)
    private Membre creePar;
}
