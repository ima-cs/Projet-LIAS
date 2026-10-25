package ma.fsbm.lias.module.membre;

import static ma.fsbm.lias.common.Views.of;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.common.CurrentUser;
import ma.fsbm.lias.config.storage.StorageService;
import ma.fsbm.lias.domain.*;
import ma.fsbm.lias.module.historique.HistoriqueService;
import ma.fsbm.lias.repository.*;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class MembreService {

  private final MembreRepository repo;
  private final HistoriqueService historique;
  private final CurrentUser user;
  private final StorageService storage;
  private final EntityManager em;

  public record Update(
    String nom,
    String prenom,
    String biographie,
    LocalDate dateNaissance,
    LocalDate dateEmbauche,
    String etablissementOrigine,
    String laboratoireOrigine,
    Set<String> centresInteret
  ) {}

  public Membre require(Long id) {
    return repo
      .findById(id)
      .orElseThrow(() -> new EntityNotFoundException("Membre introuvable"));
  }

  public Membre creer(Utilisateur u, String nom, String prenom) {
    var m = new Membre();
    m.setUtilisateur(u);
    m.setNom(nom);
    m.setPrenom(prenom);
    return repo.save(m);
  }

  private Map<String, Object> view(Membre m, boolean publicView) {
    var v = of(
      "id",
      m.getId(),
      "nom",
      m.getNom(),
      "prenom",
      m.getPrenom(),
      "biographie",
      m.getBiographie(),
      "dateEmbauche",
      m.getDateEmbauche(),
      "etablissementOrigine",
      m.getEtablissementOrigine(),
      "laboratoireOrigine",
      m.getLaboratoireOrigine(),
      "centresInteret",
      new HashSet<>(m.getCentresInteret()),
      "photoUrl",
      m.getPhotoChemin() == null
        ? null
        : "/api/public/membres/" + m.getId() + "/photo"
    );
    v.putAll(historique.etat(m.getId(), LocalDate.now()));
    if (publicView) {
      v.remove("roles");
      v.remove("membreId");
      v.remove("date");
    }
    if (!publicView) {
      v.put("email", m.getUtilisateur().getEmail());
      if (user.has("ADMIN") || user.membre().getId().equals(m.getId())) if (
        m.getDateNaissance() != null
      ) v.put("dateNaissance", m.getDateNaissance());
    }
    return v;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> detail(Long id, boolean pub) {
    return view(require(id), pub);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> list(
    String q,
    String statut,
    Long equipeId,
    int page,
    int size,
    boolean pub
  ) {
    if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException(
      "Pagination invalide"
    );
    var all = (
      q == null || q.isBlank()
        ? repo.findAll()
        : q.contains(" ")
          ? repo.findAll()
          : repo.findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCase(q, q)
    )
      .stream()
      .map(m -> view(m, pub))
      .filter(
        v ->
          q == null ||
          (v.get("nom") + " " + v.get("prenom"))
            .toLowerCase()
            .contains(q.toLowerCase())
      )
      .filter(v -> statut == null || statut.equals(v.get("statut")))
      .filter(v -> equipeId == null || equipeId.equals(v.get("equipeId")))
      .toList();
    return of(
      "content",
      all
        .stream()
        .skip((long) page * size)
        .limit(size)
        .toList(),
      "totalElements",
      all.size(),
      "totalPages",
      (all.size() + size - 1) / size,
      "number",
      page
    );
  }

  public Map<String, Object> update(Long id, Update r) {
    user.self(id);
    if (
      r.nom() == null ||
      r.nom().isBlank() ||
      r.nom().length() > 80 ||
      r.prenom() == null ||
      r.prenom().isBlank() ||
      r.prenom().length() > 80
    ) throw new IllegalArgumentException(
      "Nom et prénom requis (80 caractères maximum)"
    );
    if (
      r.dateNaissance() != null && r.dateNaissance().isAfter(LocalDate.now())
    ) throw new IllegalArgumentException("Date de naissance invalide");
    var m = require(id);
    m.setNom(r.nom().trim());
    m.setPrenom(r.prenom().trim());
    m.setBiographie(r.biographie());
    m.setDateNaissance(r.dateNaissance());
    m.setDateEmbauche(r.dateEmbauche());
    m.setEtablissementOrigine(r.etablissementOrigine());
    m.setLaboratoireOrigine(r.laboratoireOrigine());
    if (r.centresInteret() != null) {
      if (
        r
          .centresInteret()
          .stream()
          .anyMatch(x -> x == null || x.length() > 120)
      ) throw new IllegalArgumentException("Centre d'intérêt trop long");
      m.setCentresInteret(new HashSet<>(r.centresInteret()));
    }
    return view(m, false);
  }

  public Map<String, Object> photo(Long id, MultipartFile f) {
    user.self(id);
    var m = require(id);
    if (
      !Set.of("image/png", "image/jpeg").contains(f.getContentType())
    ) throw new IllegalArgumentException("Image PNG ou JPEG requise");
    m.setPhotoChemin(storage.store(f));
    return of("photoUrl", "/api/public/membres/" + id + "/photo");
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> audit(Long id) {
    user.self(id);
    require(id);
    var reader = AuditReaderFactory.get(em);
    return reader
      .getRevisions(Membre.class, id)
      .stream()
      .map(n -> {
        var m = reader.find(Membre.class, id, n);
        return of(
          "revision",
          n,
          "date",
          reader.getRevisionDate(n),
          "nom",
          m.getNom(),
          "prenom",
          m.getPrenom(),
          "biographie",
          m.getBiographie(),
          "centresInteret",
          new HashSet<>(m.getCentresInteret())
        );
      })
      .toList();
  }

  @Transactional(readOnly = true)
  public org.springframework.core.io.Resource photoFile(Long id) {
    var path = require(id).getPhotoChemin();
    if (path == null) throw new EntityNotFoundException("Aucune photo");
    return new org.springframework.core.io.FileSystemResource(
      storage.load(path)
    );
  }
}
