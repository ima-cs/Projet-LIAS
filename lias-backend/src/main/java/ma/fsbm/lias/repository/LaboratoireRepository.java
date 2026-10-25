package ma.fsbm.lias.repository;

import ma.fsbm.lias.domain.Laboratoire;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaboratoireRepository
  extends JpaRepository<Laboratoire, Long> {}
