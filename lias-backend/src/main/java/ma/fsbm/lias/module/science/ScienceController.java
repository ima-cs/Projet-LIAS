package ma.fsbm.lias.module.science;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ScienceController {

  private final ScienceService service;

  @GetMapping({ "/api/equipes", "/api/public/equipes" })
  public List<Map<String, Object>> teams() {
    return service.teams();
  }

  @GetMapping({ "/api/equipes/{id}", "/api/public/equipes/{id}" })
  public Map<String, Object> team(@PathVariable Long id) {
    return service.teamDetail(id);
  }

  @PostMapping("/api/equipes")
  public Map<String, Object> createTeam(
    @RequestBody ScienceService.TeamRequest r
  ) {
    return service.saveTeam(null, r);
  }

  @PutMapping("/api/equipes/{id}")
  public Map<String, Object> updateTeam(
    @PathVariable Long id,
    @RequestBody ScienceService.TeamRequest r
  ) {
    return service.saveTeam(id, r);
  }

  @GetMapping({
    "/api/evenements",
    "/api/public/evenements",
    "/api/calendrier",
  })
  public List<Map<String, Object>> events(
    @RequestParam(required = false) String q,
    @RequestParam(required = false) String type,
    @RequestParam(required = false) LocalDate debut,
    @RequestParam(required = false) LocalDate fin
  ) {
    return service.events(q, type, debut, fin);
  }

  @GetMapping({ "/api/evenements/{id}", "/api/public/evenements/{id}" })
  public Map<String, Object> event(@PathVariable Long id) {
    return service.eventDetail(id);
  }

  @PostMapping("/api/evenements")
  public Map<String, Object> createEvent(
    @RequestBody ScienceService.EventRequest r
  ) {
    return service.saveEvent(null, r);
  }

  @PutMapping("/api/evenements/{id}")
  public Map<String, Object> updateEvent(
    @PathVariable Long id,
    @RequestBody ScienceService.EventRequest r
  ) {
    return service.saveEvent(id, r);
  }

  @GetMapping({ "/api/publications", "/api/public/publications" })
  public List<Map<String, Object>> pubs(
    @RequestParam(required = false) String q,
    @RequestParam(required = false) Integer annee,
    @RequestParam(required = false) String type,
    @RequestParam(required = false) Long equipeId,
    @RequestParam(required = false) Long auteurId
  ) {
    return service.publications(q, annee, type, equipeId, auteurId);
  }

  @GetMapping({ "/api/publications/{id}", "/api/public/publications/{id}" })
  public Map<String, Object> pub(@PathVariable Long id) {
    return service.publication(id);
  }

  @PostMapping("/api/publications")
  public Map<String, Object> createPub(
    @RequestBody ScienceService.PubRequest r
  ) {
    return service.savePublication(null, r);
  }

  @PutMapping("/api/publications/{id}")
  public Map<String, Object> updatePub(
    @PathVariable Long id,
    @RequestBody ScienceService.PubRequest r
  ) {
    return service.savePublication(id, r);
  }
}
