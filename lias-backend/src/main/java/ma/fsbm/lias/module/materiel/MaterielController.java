package ma.fsbm.lias.module.materiel;

import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.domain.enums.StatutBesoin;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MaterielController {

  private final MaterielService service;

  public record Decision(StatutBesoin statut) {}

  @GetMapping("/materiels")
  public List<Map<String, Object>> stock() {
    return service.stock();
  }

  @PostMapping("/materiels")
  public Map<String, Object> arrive(@RequestBody MaterielService.Arrivage r) {
    return service.arrive(r);
  }

  @GetMapping("/besoins")
  public List<Map<String, Object>> needs() {
    return service.needs();
  }

  @PostMapping("/besoins")
  public Map<String, Object> need(@RequestBody MaterielService.Besoin r) {
    return service.need(r);
  }

  @PatchMapping("/besoins/{id}/decision")
  public Map<String, Object> decide(
    @PathVariable Long id,
    @RequestBody Decision r
  ) {
    return service.decide(id, r.statut());
  }

  @PostMapping("/distributions")
  public Map<String, Object> distribute(
    @RequestBody MaterielService.Distribution r
  ) {
    return service.distribute(r);
  }

  @GetMapping("/materiels/equite")
  public List<Map<String, Object>> equity() {
    return service.equity();
  }
}
