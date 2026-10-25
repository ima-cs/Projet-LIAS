package ma.fsbm.lias.repository;

import ma.fsbm.lias.domain.DistributionMateriel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistributionMaterielRepository
  extends JpaRepository<DistributionMateriel, Long> {}
