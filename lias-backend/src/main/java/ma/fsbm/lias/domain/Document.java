package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.fsbm.lias.domain.enums.TypeDocument;

import java.time.LocalDateTime;

/**
 * Métadonnées d'un fichier archivé. Le binaire est stocké HORS base (cheminStockage).
 * Versionnement : nouvel upload = nouvelle ligne (version incrémentée) reliée par documentParent.
 */
@Entity
@Table(name = "document")
@Getter @Setter @NoArgsConstructor
public class Document {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 30)
    private TypeDocument typeDocument;

    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(name = "chemin_stockage", nullable = false, length = 500)
    private String cheminStockage;

    @Column(name = "type_mime", length = 100)
    private String typeMime;

    @Column(name = "taille_octets")
    private Long tailleOctets;

    @Column(nullable = false)
    private Integer version = 1;

    /** Document remplacé par cette version (chaîne de versions). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_parent_id")
    private Document documentParent;

    /** Événement de rattachement (null pour documents hors événement : PV, administratif…). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id")
    private Evenement evenement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_membre_id", nullable = false)
    private Membre uploadedBy;

    @Column(name = "date_upload", nullable = false)
    private LocalDateTime dateUpload = LocalDateTime.now();
}
