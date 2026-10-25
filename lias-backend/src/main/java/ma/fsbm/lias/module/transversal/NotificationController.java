package ma.fsbm.lias.module.transversal;

import static ma.fsbm.lias.common.Views.of;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService service;

  @GetMapping
  public List<Map<String, Object>> list() {
    return service.list();
  }

  @GetMapping("/non-lues")
  public Map<String, Object> count() {
    return of(
      "nombre",
      service
        .list()
        .stream()
        .filter(n -> Boolean.FALSE.equals(n.get("lu")))
        .count()
    );
  }

  @PatchMapping("/{id}/lire")
  public void read(@PathVariable Long id) {
    service.read(id);
  }

  @PatchMapping("/tout-lire")
  public void all() {
    service.list().forEach(n -> service.read((Long) n.get("id")));
  }
}
