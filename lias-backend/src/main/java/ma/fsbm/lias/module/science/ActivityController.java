package ma.fsbm.lias.module.science;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ActivityController {

  private final ActivityService service;

  @GetMapping("/reunions")
  public List<Map<String, Object>> list() {
    return service.meetings();
  }

  @PostMapping("/reunions")
  public Map<String, Object> create(@RequestBody ActivityService.Meeting r) {
    return service.meeting(r);
  }

  @PutMapping("/reunions/{id}/pv")
  public Map<String, Object> pv(
    @PathVariable Long id,
    @RequestParam MultipartFile file
  ) {
    return service.pv(id, file);
  }

  @GetMapping("/rapports")
  public List<Map<String, Object>> reports() {
    return service.reports();
  }

  @PostMapping("/rapports/{annee}/generer")
  public Map<String, Object> generate(@PathVariable int annee) {
    return service.generate(annee);
  }
}
