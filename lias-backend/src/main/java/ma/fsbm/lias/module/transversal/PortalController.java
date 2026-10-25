package ma.fsbm.lias.module.transversal;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PortalController {

  private final PortalService service;

  @GetMapping("/api/public/portail")
  public Map<String, Object> portal() {
    return service.portal();
  }

  @GetMapping("/api/recherche")
  public List<Map<String, Object>> search(@RequestParam String q) {
    return service.search(q);
  }
}
