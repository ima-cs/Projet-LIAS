package ma.fsbm.lias.repository;

import java.util.Optional;
import ma.fsbm.lias.domain.HistoriqueStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HistoriqueStatutRepository
  extends JpaRepository<HistoriqueStatut, Long>
{
  @Query(
    value = """
    SELECT type_statut
    FROM historique_statut
    WHERE membre_id = :membreId
      AND date_fin IS NULL
    LIMIT 1
    """,
    nativeQuery = true
  )
  Optional<String> findStatutCourant(@Param("membreId") Long membreId);
}
