package ma.fsbm.lias.repository;

import java.util.Optional;
import ma.fsbm.lias.domain.Membre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembreRepository extends JpaRepository<Membre, Long> {
  java.util.List<Membre> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(
    String nom,
    String prenom
  );
  Optional<Membre> findByUtilisateur_Id(Long utilisateurId);
}
