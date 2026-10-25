package ma.fsbm.lias.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import ma.fsbm.lias.module.auth.InvitationCreated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Local invitation outbox for demonstrations without an e-mail provider. */
@Component
@Profile("demo & !test")
public class DemoInvitationDelivery {

  private final Path outbox;

  public DemoInvitationDelivery(@Value("${lias.storage.root}") String root) {
    outbox = Path.of(root).resolve("invitations-demo.txt");
  }

  @TransactionalEventListener
  public synchronized void deliver(InvitationCreated e) {
    try {
      Files.createDirectories(outbox.toAbsolutePath().getParent());
      Files.writeString(
        outbox,
        e.email() +
          "\nhttp://127.0.0.1:5173/activation?token=" +
          e.token() +
          "\n\n",
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND
      );
    } catch (java.io.IOException ex) {
      org.slf4j.LoggerFactory.getLogger(getClass()).error(
        "Impossible d'écrire l'invitation locale. Vérifier les permissions du stockage."
      );
    }
  }
}
