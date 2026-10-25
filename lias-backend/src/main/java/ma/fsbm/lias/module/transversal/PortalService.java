package ma.fsbm.lias.module.transversal;

import static ma.fsbm.lias.common.Views.of;

import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.common.CurrentUser;
import ma.fsbm.lias.module.membre.MembreService;
import ma.fsbm.lias.module.science.*;
import ma.fsbm.lias.repository.LaboratoireRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalService {

  private final LaboratoireRepository lab;
  private final ScienceService science;
  private final MembreService members;
  private final DocumentService documents;
  private final CurrentUser user;

  public Map<String, Object> portal() {
    return of(
      "laboratoire",
      lab
        .findAll()
        .stream()
        .findFirst()
        .map(l ->
          of(
            "nom",
            l.getNom(),
            "sigle",
            l.getSigle(),
            "presentation",
            l.getPresentation(),
            "dateCreation",
            l.getDateCreationLabo()
          )
        )
        .orElse(Map.of()),
      "equipes",
      science.teams(),
      "evenements",
      science.events(null, null, null, null),
      "publications",
      science.publications(null, null, null, null, null)
    );
  }

  public List<Map<String, Object>> search(String q) {
    if (q == null || q.trim().length() < 2) throw new IllegalArgumentException(
      "Deux caractères minimum"
    );
    List<Map<String, Object>> out = new ArrayList<>();
    var ms = (List<Map<String, Object>>) members
      .list(q, null, null, 0, 100, false)
      .get("content");
    ms.forEach(m ->
      out.add(
        of(
          "type",
          "MEMBRE",
          "id",
          m.get("id"),
          "titre",
          m.get("prenom") + " " + m.get("nom"),
          "description",
          m.get("biographie")
        )
      )
    );
    science
      .events(q, null, null, null)
      .forEach(e ->
        out.add(
          of(
            "type",
            "EVENEMENT",
            "id",
            e.get("id"),
            "titre",
            e.get("titre"),
            "description",
            e.get("description")
          )
        )
      );
    science
      .publications(q, null, null, null, null)
      .forEach(p ->
        out.add(
          of(
            "type",
            "PUBLICATION",
            "id",
            p.get("id"),
            "titre",
            p.get("titre"),
            "description",
            p.get("annee")
          )
        )
      );
    if (user.has("PERMANENT")) documents
      .list(q, null, null, null)
      .forEach(d ->
        out.add(
          of(
            "type",
            "DOCUMENT",
            "id",
            d.get("id"),
            "titre",
            d.get("nomFichier"),
            "description",
            d.get("typeDocument")
          )
        )
      );
    return out;
  }
}
