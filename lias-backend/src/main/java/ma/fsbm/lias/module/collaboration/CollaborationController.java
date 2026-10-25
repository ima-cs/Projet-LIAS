package ma.fsbm.lias.module.collaboration;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CollaborationController {

  private final CollaborationService service;

  public record MessageRequest(String contenu) {}

  @GetMapping("/partenaires")
  public List<Map<String, Object>> partners() {
    return service.partners();
  }

  @PostMapping("/partenaires")
  public Map<String, Object> partner(
    @RequestBody CollaborationService.Partner r
  ) {
    return service.partner(null, r);
  }

  @PutMapping("/partenaires/{id}")
  public Map<String, Object> partner(
    @PathVariable Long id,
    @RequestBody CollaborationService.Partner r
  ) {
    return service.partner(id, r);
  }

  @GetMapping("/conventions")
  public List<Map<String, Object>> agreements() {
    return service.agreements();
  }

  @PostMapping("/conventions")
  public Map<String, Object> agreement(
    @RequestBody CollaborationService.Agreement r
  ) {
    return service.agreement(null, r);
  }

  @PutMapping("/conventions/{id}")
  public Map<String, Object> agreement(
    @PathVariable Long id,
    @RequestBody CollaborationService.Agreement r
  ) {
    return service.agreement(id, r);
  }

  @GetMapping("/discussions")
  public List<Map<String, Object>> threads() {
    return service.threads();
  }

  @PostMapping("/discussions")
  public Map<String, Object> thread(
    @RequestBody CollaborationService.Thread r
  ) {
    return service.thread(r);
  }

  @GetMapping("/discussions/{id}/messages")
  public List<Map<String, Object>> messages(@PathVariable Long id) {
    return service.messages(id);
  }

  @PostMapping("/discussions/{id}/messages")
  public Map<String, Object> message(
    @PathVariable Long id,
    @RequestBody MessageRequest r
  ) {
    return service.message(id, r.contenu());
  }
}
