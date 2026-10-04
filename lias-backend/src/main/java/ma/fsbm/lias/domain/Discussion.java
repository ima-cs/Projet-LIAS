package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.fsbm.lias.domain.enums.TypeDiscussion;

import java.time.LocalDateTime;

/** Fil de discussion : générale, liée à une équipe, ou liée à un événement. */
@Entity
@Table(name = "discussion")
@Getter @Setter @NoArgsConstructor
public class Discussion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_discussion", nullable = false, length = 20)
    private TypeDiscussion typeDiscussion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id")
    private Equipe equipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id")
    private Evenement evenement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cree_par_membre_id", nullable = false)
    private Membre creePar;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
