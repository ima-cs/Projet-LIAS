package ma.fsbm.lias.repository;

import ma.fsbm.lias.domain.Evenement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvenementRepository extends JpaRepository<Evenement, Long> {
  java.util.List<Evenement> findByTitreContainingIgnoreCase(String q);
}
