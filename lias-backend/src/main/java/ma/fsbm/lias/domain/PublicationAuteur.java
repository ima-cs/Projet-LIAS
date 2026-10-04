package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Association n-n Publication <-> Membre, porteuse de l'ordre de signature.
 * Préserve l'attribution même après le départ d'un auteur (aucune suppression de membre).
 */
@Entity
@Table(name = "publication_auteur",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_pub_auteur", columnNames = {"publication_id", "membre_id"}))
@Getter @Setter @NoArgsConstructor
public class PublicationAuteur {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(name = "ordre_auteur", nullable = false)
    private Integer ordreAuteur;
}
