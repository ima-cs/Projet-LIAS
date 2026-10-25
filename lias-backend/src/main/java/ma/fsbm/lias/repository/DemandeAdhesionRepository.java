package ma.fsbm.lias.repository;

import ma.fsbm.lias.domain.DemandeAdhesion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandeAdhesionRepository
  extends JpaRepository<DemandeAdhesion, Long> {}
