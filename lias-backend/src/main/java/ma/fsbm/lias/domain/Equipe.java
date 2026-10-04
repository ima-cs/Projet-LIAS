package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipe",
       uniqueConstraints = @UniqueConstraint(name = "uk_equipe_nom", columnNames = "nom"))
@Getter @Setter @NoArgsConstructor
public class Equipe {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nom;

    @Column(length = 200)
    private String thematique;

    @Column(columnDefinition = "text")
    private String description;
}
