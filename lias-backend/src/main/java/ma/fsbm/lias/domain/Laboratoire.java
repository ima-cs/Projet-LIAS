package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Données institutionnelles (enregistrement unique / singleton). */
@Entity
@Table(name = "laboratoire")
@Getter @Setter @NoArgsConstructor
public class Laboratoire {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(length = 30)
    private String sigle;

    @Column(name = "date_creation_labo")
    private LocalDate dateCreationLabo;

    @Column(columnDefinition = "text")
    private String presentation;
}
