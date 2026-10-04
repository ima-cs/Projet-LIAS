package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.fsbm.lias.domain.enums.TypePartenaire;

@Entity
@Table(name = "partenaire")
@Getter @Setter @NoArgsConstructor
public class Partenaire {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_partenaire", nullable = false, length = 20)
    private TypePartenaire typePartenaire;

    @Column(name = "contact_email", length = 180)
    private String contactEmail;

    @Column(length = 100)
    private String pays;

    @Column(columnDefinition = "text")
    private String description;
}
