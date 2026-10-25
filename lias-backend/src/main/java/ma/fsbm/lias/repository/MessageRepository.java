package ma.fsbm.lias.repository;

import ma.fsbm.lias.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {}
