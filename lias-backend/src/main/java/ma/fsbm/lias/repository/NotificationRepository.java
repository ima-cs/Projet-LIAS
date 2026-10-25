package ma.fsbm.lias.repository;

import ma.fsbm.lias.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository
  extends JpaRepository<Notification, Long> {}
