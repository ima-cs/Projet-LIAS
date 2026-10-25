package ma.fsbm.lias.module.membre;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class MembreController {

  private final MembreService service;

  @GetMapping("/api/membres")
  public Map<String, Object> list(
    @RequestParam(required = false) String q,
    @RequestParam(required = false) String statut,
    @RequestParam(required = false) Long equipeId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return service.list(q, statut, equipeId, page, size, false);
  }

  @GetMapping("/api/public/membres")
  public Map<String, Object> pubList(
    @RequestParam(required = false) String q,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    return service.list(q, null, null, page, size, true);
  }

  @GetMapping("/api/membres/{id}")
  public Map<String, Object> detail(@PathVariable Long id) {
    return service.detail(id, false);
  }

  @GetMapping("/api/public/membres/{id}")
  public Map<String, Object> pub(@PathVariable Long id) {
    return service.detail(id, true);
  }

  @PutMapping("/api/membres/{id}")
  public Map<String, Object> update(
    @PathVariable Long id,
    @RequestBody MembreService.Update r
  ) {
    return service.update(id, r);
  }

  @PutMapping("/api/membres/{id}/photo")
  public Map<String, Object> photo(
    @PathVariable Long id,
    @RequestParam MultipartFile file
  ) {
    return service.photo(id, file);
  }

  @GetMapping("/api/public/membres/{id}/photo")
  public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> photo(
    @PathVariable Long id
  ) throws java.io.IOException {
    var r = service.photoFile(id);
    return org.springframework.http.ResponseEntity.ok()
      .header("X-Content-Type-Options", "nosniff")
      .contentType(
        org.springframework.http.MediaType.parseMediaType(
          java.nio.file.Files.probeContentType(r.getFile().toPath())
        )
      )
      .body(r);
  }

  @GetMapping("/api/membres/{id}/historique")
  public List<Map<String, Object>> audit(@PathVariable Long id) {
    return service.audit(id);
  }
}
