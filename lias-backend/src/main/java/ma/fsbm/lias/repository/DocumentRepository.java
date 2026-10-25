package ma.fsbm.lias.repository;

import ma.fsbm.lias.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
  java.util.List<Document> findByNomFichierContainingIgnoreCase(String q);
}
