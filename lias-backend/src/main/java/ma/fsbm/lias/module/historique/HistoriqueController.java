package ma.fsbm.lias.module.historique;

import static ma.fsbm.lias.common.Views.of;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.domain.enums.*;
import ma.fsbm.lias.repository.MandatRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class HistoriqueController {

  private final HistoriqueService service;
  private final MandatRepository mandats;

  public record Statut(TypeStatut type, LocalDate dateDebut) {}

  public record Aff(String action, LocalDate date, TypeStatut type) {}

  public record Team(Long equipeId, LocalDate dateDebut) {}

  public record Role(
    TypeRole type,
    Long equipeId,
    Long mandatId,
    LocalDate dateDebut
  ) {}

  public record Fin(LocalDate dateFin) {}

  public record MandatRequest(String libelle, LocalDate dateDebut) {}

  @PostMapping("/membres/{id}/statut")
  @PreAuthorize("hasRole('ADMIN')")
  public void statut(@PathVariable Long id, @RequestBody Statut r) {
    service.statut(id, r.type(), r.dateDebut());
  }

  @PostMapping("/membres/{id}/affiliation")
  @PreAuthorize("hasRole('ADMIN')")
  public void aff(@PathVariable Long id, @RequestBody Aff r) {
    service.affiliation(id, r.action(), r.date(), r.type());
  }

  @PostMapping("/membres/{id}/equipe")
  @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
  public void team(@PathVariable Long id, @RequestBody Team r) {
    service.equipe(id, r.equipeId(), r.dateDebut());
  }

  @PostMapping("/membres/{id}/roles")
  @PreAuthorize("hasRole('DIRECTEUR')")
  public Map<String, Object> role(@PathVariable Long id, @RequestBody Role r) {
    return service.role(
      id,
      r.type(),
      r.equipeId(),
      r.mandatId(),
      r.dateDebut()
    );
  }

  @PatchMapping("/membres/{id}/roles/{roleId}/cloturer")
  @PreAuthorize("hasRole('DIRECTEUR')")
  public void close(
    @PathVariable Long id,
    @PathVariable Long roleId,
    @RequestBody Fin r
  ) {
    service.closeRole(id, roleId, r.dateFin());
  }

  @GetMapping("/membres/{id}/etat")
  public Map<String, Object> etat(
    @PathVariable Long id,
    @RequestParam LocalDate date
  ) {
    return service.etat(id, date);
  }

  @GetMapping("/membres/{id}/periodes")
  public Map<String, Object> periods(@PathVariable Long id) {
    return service.periodes(id);
  }

  @GetMapping("/mandats")
  @PreAuthorize("hasAnyRole('ADMIN','DIRECTEUR')")
  public List<Map<String, Object>> mandats() {
    return mandats
      .findAll()
      .stream()
      .map(m ->
        of(
          "id",
          m.getId(),
          "libelle",
          m.getLibelle(),
          "dateDebut",
          m.getDateDebut(),
          "dateFin",
          m.getDateFin()
        )
      )
      .toList();
  }

  @PostMapping("/mandats")
  @PreAuthorize("hasRole('ADMIN')")
  public Map<String, Object> mandat(@RequestBody MandatRequest r) {
    return service.mandat(r.libelle(), r.dateDebut());
  }

  @PatchMapping("/mandats/{id}/cloturer")
  @PreAuthorize("hasRole('ADMIN')")
  public void closeMandat(@PathVariable Long id, @RequestBody Fin r) {
    service.closeMandat(id, r.dateFin());
  }
}
