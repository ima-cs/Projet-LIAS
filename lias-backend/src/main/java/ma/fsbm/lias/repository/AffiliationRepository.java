package ma.fsbm.lias.repository;

import ma.fsbm.lias.domain.Affiliation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AffiliationRepository
  extends JpaRepository<Affiliation, Long> {}
