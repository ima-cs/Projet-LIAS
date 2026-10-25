package ma.fsbm.lias.module.gouvernance;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AdhesionController {

  private final AdhesionService service;

  @PostMapping(value = "/api/public/adhesions", consumes = "application/json")
  public Map<String, Object> submit(@RequestBody AdhesionService.Request r) {
    return service.submit(r);
  }

  @PostMapping(
    value = "/api/public/adhesions",
    consumes = "multipart/form-data"
  )
  public Map<String, Object> submitFile(
    @RequestPart("demande") AdhesionService.Request r,
    @RequestPart(
      value = "file",
      required = false
    ) org.springframework.web.multipart.MultipartFile f
  ) {
    return service.submitWithCv(r, f);
  }

  @GetMapping("/api/adhesions")
  public List<Map<String, Object>> list() {
    return service.list();
  }

  @GetMapping("/api/adhesions/{id}")
  public Map<String, Object> detail(@PathVariable Long id) {
    return service
      .list()
      .stream()
      .filter(d -> id.equals(d.get("id")))
      .findFirst()
      .orElseThrow(() ->
        new jakarta.persistence.EntityNotFoundException("Demande introuvable")
      );
  }

  @PostMapping("/api/adhesions/{id}/decision")
  public Map<String, Object> decide(
    @PathVariable Long id,
    @RequestBody AdhesionService.Decision r
  ) {
    return service.decision(id, r);
  }
}
