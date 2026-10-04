package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.fsbm.lias.domain.enums.TypeNotification;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter @Setter @NoArgsConstructor
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinataire_membre_id", nullable = false)
    private Membre destinataire;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_notification", nullable = false, length = 30)
    private TypeNotification typeNotification;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "text")
    private String contenu;

    /** Lien applicatif optionnel (ex. /evenements/12). */
    @Column(length = 255)
    private String lien;

    @Column(nullable = false)
    private boolean lu = false;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
