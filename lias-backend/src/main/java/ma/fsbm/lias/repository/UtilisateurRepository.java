package ma.fsbm.lias.repository;

import java.util.Optional;
import ma.fsbm.lias.domain.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository
  extends JpaRepository<Utilisateur, Long>
{
  Optional<Utilisateur> findByEmailIgnoreCase(String email);

  boolean existsByTokenActivation(String tokenActivation);

  @org.springframework.data.jpa.repository.Lock(
    jakarta.persistence.LockModeType.PESSIMISTIC_WRITE
  )
  Optional<Utilisateur> findByTokenActivation(String tokenActivation);
}
