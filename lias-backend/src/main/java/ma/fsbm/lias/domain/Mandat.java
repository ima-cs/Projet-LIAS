package ma.fsbm.lias.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Période de gouvernance. Borne les rôles directeur / vice-directeur / chefs d'équipe
 * (référencés via historique_role.mandat_id). Mandat ouvert (dateFin == null) = mandat actif.
 */
@Entity
@Table(name = "mandat")
@Getter @Setter @NoArgsConstructor
public class Mandat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 120)
    private String libelle;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;
}
