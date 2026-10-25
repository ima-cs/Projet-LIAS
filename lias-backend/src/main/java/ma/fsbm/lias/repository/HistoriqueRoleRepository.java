package ma.fsbm.lias.repository;

import ma.fsbm.lias.domain.HistoriqueRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistoriqueRoleRepository
  extends JpaRepository<HistoriqueRole, Long>
{
  @Query(
    value = """
    SELECT EXISTS (
        SELECT 1
        FROM historique_role hr
        JOIN mandat m ON m.id = hr.mandat_id
        WHERE hr.membre_id = :membreId
          AND hr.type_role = 'DIRECTEUR'
          AND hr.date_fin IS NULL
          AND hr.date_debut <= CURRENT_DATE
          AND m.date_debut <= CURRENT_DATE
          AND m.date_fin IS NULL
    )
    """,
    nativeQuery = true
  )
  boolean estDirecteurActif(@Param("membreId") Long membreId);
}
