package ma.fsbm.lias.module.science;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.domain.enums.TypeDocument;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/documents")
public class DocumentController {

  private final DocumentService service;

  @GetMapping
  public List<Map<String, Object>> list(
    @RequestParam(required = false) String q,
    @RequestParam(required = false) TypeDocument type,
    @RequestParam(required = false) Long evenementId,
    @RequestParam(required = false) LocalDate date
  ) {
    return service.list(q, type, evenementId, date);
  }

  @PostMapping
  public Map<String, Object> upload(
    @RequestParam MultipartFile file,
    @RequestParam TypeDocument type,
    @RequestParam(required = false) Long evenementId,
    @RequestParam(required = false) Long parentId
  ) {
    return service.view(service.store(file, type, evenementId, parentId));
  }

  @GetMapping("/{id}/telecharger")
  public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> download(
    @PathVariable Long id
  ) {
    return service.download(id);
  }
}
