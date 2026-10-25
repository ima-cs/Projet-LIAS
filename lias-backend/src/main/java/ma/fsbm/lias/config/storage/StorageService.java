package ma.fsbm.lias.config.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {

  private static final long MAX_SIZE = 20 * 1024 * 1024;

  private static final Set<String> ALLOWED_TYPES = Set.of(
    "application/pdf",
    "image/jpeg",
    "image/png"
  );

  private final Path root;

  public StorageService(
    @Value("${lias.storage.root:storage}") String rootDirectory
  ) {
    this.root = Path.of(rootDirectory).toAbsolutePath().normalize();
  }

  public String store(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Le fichier est vide.");
    }

    if (file.getSize() > MAX_SIZE) {
      throw new IllegalArgumentException(
        "Le fichier dépasse la taille maximale de 20 Mo."
      );
    }

    if (
      file.getContentType() == null ||
      !ALLOWED_TYPES.contains(file.getContentType())
    ) {
      throw new IllegalArgumentException("Type de fichier non autorisé.");
    }

    try (var input = file.getInputStream()) {
      byte[] signature = input.readNBytes(8);
      boolean valid = switch (file.getContentType()) {
        case "application/pdf" -> signature.length >= 5 &&
          new String(
            signature,
            0,
            5,
            java.nio.charset.StandardCharsets.US_ASCII
          ).equals("%PDF-");
        case "image/png" -> signature.length == 8 &&
          java.util.Arrays.equals(signature, new byte[] {
            (byte) 137,
            80,
            78,
            71,
            13,
            10,
            26,
            10,
          });
        case "image/jpeg" -> signature.length >= 3 &&
          signature[0] == (byte) 255 &&
          signature[1] == (byte) 216 &&
          signature[2] == (byte) 255;
        default -> false;
      };
      if (!valid) throw new IllegalArgumentException(
        "Le contenu du fichier ne correspond pas à son type MIME."
      );
    } catch (IOException e) {
      throw new IllegalArgumentException("Fichier illisible.");
    }
    String extension = switch (file.getContentType()) {
      case "application/pdf" -> ".pdf";
      case "image/png" -> ".png";
      default -> ".jpg";
    };

    String internalName = UUID.randomUUID() + extension.toLowerCase();

    try {
      Files.createDirectories(root);

      Path destination = root.resolve(internalName).normalize();

      if (!destination.startsWith(root)) {
        throw new IllegalArgumentException("Chemin de fichier invalide.");
      }

      try (var input = file.getInputStream()) {
        Files.copy(input, destination);
      }
      if (
        org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()
      ) {
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
          new org.springframework.transaction.support.TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              if (status == STATUS_ROLLED_BACK) {
                try {
                  Files.deleteIfExists(destination);
                } catch (IOException ex) {
                  org.slf4j.LoggerFactory.getLogger(StorageService.class).warn(
                    "Nettoyage d'un fichier non archivé impossible."
                  );
                }
              }
            }
          }
        );
      }

      return internalName;
    } catch (IOException exception) {
      throw new IllegalStateException(
        "Impossible d'enregistrer le fichier.",
        exception
      );
    }
  }

  public Path load(String internalName) {
    Path file = root.resolve(internalName).normalize();

    if (!file.startsWith(root)) {
      throw new IllegalArgumentException("Chemin de fichier invalide.");
    }

    if (!Files.exists(file)) {
      throw new IllegalArgumentException("Fichier introuvable.");
    }

    return file;
  }
}
