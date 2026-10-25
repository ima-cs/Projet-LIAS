package ma.fsbm.lias.repository;

import ma.fsbm.lias.domain.Publication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationRepository
  extends JpaRepository<Publication, Long>
{
  java.util.List<Publication> findByTitreContainingIgnoreCase(String q);
}
