package ma.fsbm.lias.module.science;

import static ma.fsbm.lias.common.Views.of;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.common.CurrentUser;
import ma.fsbm.lias.config.storage.StorageService;
import ma.fsbm.lias.domain.*;
import ma.fsbm.lias.domain.enums.*;
import ma.fsbm.lias.module.transversal.BusinessEvent;
import ma.fsbm.lias.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

  private final jakarta.persistence.EntityManager em;
  private final ma.fsbm.lias.repository.HistoriqueRoleRepository roles;
  private final ma.fsbm.lias.repository.MembreRepository members;
  private final DocumentRepository repo;
  private final StorageService storage;
  private final CurrentUser user;
  private final ScienceService science;
  private final ApplicationEventPublisher publisher;

  public Document require(Long id) {
    return repo
      .findById(id)
      .orElseThrow(() ->
        new jakarta.persistence.EntityNotFoundException("Document introuvable")
      );
  }

  private boolean readable(Document d) {
    return (
      d.getTypeDocument() != TypeDocument.CV ||
      user.has("DIRECTEUR") ||
      d.getUploadedBy().getId().equals(user.membre().getId())
    );
  }

  public Map<String, Object> view(Document d) {
    return of(
      "id",
      d.getId(),
      "nomFichier",
      d.getNomFichier(),
      "typeDocument",
      d.getTypeDocument(),
      "typeMime",
      d.getTypeMime(),
      "tailleOctets",
      d.getTailleOctets(),
      "version",
      d.getVersion(),
      "documentParentId",
      d.getDocumentParent() == null ? null : d.getDocumentParent().getId(),
      "evenementId",
      d.getEvenement() == null ? null : d.getEvenement().getId(),
      "dateUpload",
      d.getDateUpload(),
      "uploadedBy",
      d.getUploadedBy().getPrenom() + " " + d.getUploadedBy().getNom()
    );
  }

  public List<Map<String, Object>> list(
    String q,
    TypeDocument type,
    Long evenementId,
    LocalDate date
  ) {
    user.internal();
    return (
      q == null || q.isBlank()
        ? repo.findAll()
        : repo.findByNomFichierContainingIgnoreCase(q)
    )
      .stream()
      .filter(
        d ->
          q == null || d.getNomFichier().toLowerCase().contains(q.toLowerCase())
      )
      .filter(this::readable)
      .filter(d -> type == null || d.getTypeDocument() == type)
      .filter(
        d ->
          evenementId == null ||
          (d.getEvenement() != null &&
            evenementId.equals(d.getEvenement().getId()))
      )
      .filter(d -> date == null || d.getDateUpload().toLocalDate().equals(date))
      .sorted(Comparator.comparing(Document::getDateUpload).reversed())
      .map(this::view)
      .toList();
  }

  public Document store(
    MultipartFile f,
    TypeDocument type,
    Long eventId,
    Long parentId
  ) {
    user.internal();
    if (type == null) throw new IllegalArgumentException("Type requis");
    Document parent = parentId == null ? null : require(parentId);
    if (
      parent != null && !readable(parent)
    ) throw new org.springframework.security.access.AccessDeniedException(
      "Document privé"
    );
    if (parent != null) em.lock(
      parent,
      jakarta.persistence.LockModeType.PESSIMISTIC_WRITE
    );
    var event = eventId == null ? null : science.event(eventId);
    if (
      parent != null &&
      repo
        .findAll()
        .stream()
        .anyMatch(
          d ->
            d.getDocumentParent() != null &&
            parentId.equals(d.getDocumentParent().getId())
        )
    ) throw new ma.fsbm.lias.common.error.ConflictException(
      "Une version suivante existe déjà"
    );
    var d = new Document();
    d.setTypeDocument(type);
    d.setNomFichier(
      org.springframework.util.StringUtils.cleanPath(
        Objects.requireNonNullElse(f.getOriginalFilename(), "document")
      )
    );
    d.setCheminStockage(storage.store(f));
    d.setTypeMime(f.getContentType());
    d.setTailleOctets(f.getSize());
    d.setUploadedBy(user.membre());
    d.setEvenement(event);
    d.setDocumentParent(parent);
    d.setVersion(parent == null ? 1 : parent.getVersion() + 1);
    repo.saveAndFlush(d);
    publisher.publishEvent(
      new BusinessEvent(
        TypeNotification.DOCUMENT_AJOUTE,
        "Document ajouté",
        d.getNomFichier(),
        "/documents",
        null,
        false
      )
    );
    return d;
  }

  public Document publicCv(MultipartFile f) {
    if (
      !"application/pdf".equals(f.getContentType())
    ) throw new IllegalArgumentException("Le CV doit être au format PDF");
    var owner = members
      .findAll()
      .stream()
      .filter(m -> roles.estDirecteurActif(m.getId()))
      .findFirst()
      .orElseThrow(() ->
        new ma.fsbm.lias.common.error.ConflictException(
          "Aucune direction active pour recevoir les dossiers"
        )
      );
    var d = new Document();
    d.setTypeDocument(TypeDocument.CV);
    d.setNomFichier("CV-" + java.util.UUID.randomUUID() + ".pdf");
    d.setCheminStockage(storage.store(f));
    d.setTypeMime("application/pdf");
    d.setTailleOctets(f.getSize());
    d.setUploadedBy(owner);
    return repo.save(d);
  }

  public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> download(
    Long id
  ) {
    user.internal();
    var d = require(id);
    if (
      !readable(d)
    ) throw new org.springframework.security.access.AccessDeniedException(
      "Document privé"
    );
    return org.springframework.http.ResponseEntity.ok()
      .header("X-Content-Type-Options", "nosniff")
      .header(
        "Content-Disposition",
        org.springframework.http.ContentDisposition.attachment()
          .filename(d.getNomFichier(), java.nio.charset.StandardCharsets.UTF_8)
          .build()
          .toString()
      )
      .contentType(
        org.springframework.http.MediaType.parseMediaType(d.getTypeMime())
      )
      .body(
        new org.springframework.core.io.FileSystemResource(
          storage.load(d.getCheminStockage())
        )
      );
  }
}
