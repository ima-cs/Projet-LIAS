package ma.fsbm.lias.config.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

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

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "Type de fichier non autorisé."
            );
        }

        String originalName = file.getOriginalFilename();

        String extension = "";

        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(
                    originalName.lastIndexOf(".")
            );
        }

        String internalName =
                UUID.randomUUID() + extension.toLowerCase();

        try {
            Files.createDirectories(root);

            Path destination = root.resolve(internalName).normalize();

            if (!destination.startsWith(root)) {
                throw new IllegalArgumentException(
                        "Chemin de fichier invalide."
                );
            }

            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

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
            throw new IllegalArgumentException(
                    "Chemin de fichier invalide."
            );
        }

        if (!Files.exists(file)) {
            throw new IllegalArgumentException(
                    "Fichier introuvable."
            );
        }

        return file;
    }
}