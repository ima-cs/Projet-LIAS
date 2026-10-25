package ma.fsbm.lias.repository;

import ma.fsbm.lias.domain.PublicationAuteur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationAuteurRepository
  extends JpaRepository<PublicationAuteur, Long> {}
