# Plan de construction ordonné + « context packs »

Principe directeur : **une phase = une session IA**. À chaque session, on fournit un *context pack*
minimal et **stable**, on délimite le périmètre, et on interdit explicitement de toucher au reste.
C'est ce qui empêche l'IA de déborder, de réinventer le modèle ou de mélanger les modules.

## Règles valables pour TOUTES les sessions

À fournir systématiquement (le « socle de contexte ») :
- `00_README.md` (décisions, hypothèses, structure, règle anti-mélange) ;
- le dossier `domain/` (entités + enums) **et** `V1__init_schema.sql` — marqués **LECTURE SEULE**.

Garde-fous à répéter à l'IA :
1. **Ne pas modifier** les entités existantes ni `V1`. Toute évolution du modèle = **nouvelle
   migration** `V2__...sql` + ajustement d'entité, justifié.
2. **Rester dans le paquet du module** courant (`module/<nom>/`). Pour lire/écrire une entité
   partagée, **appeler le service propriétaire**, ne pas dupliquer la logique.
3. **DTO obligatoires** en entrée/sortie d'API ; pas d'entité exposée.
4. Respecter la **matrice d'autorisation** et le **patron temporel** (période ouverte = courante).
5. Livrer avec **tests** couvrant les `CA` du module, et s'arrêter à la **Definition of Done**.

---

## Phase 0 — Socle technique
**Objectif** : un backend qui démarre, migre la base et expose la sécurité de base.
**Dépend de** : rien.
**Périmètre** : projet Maven (déjà fourni), `application.yml`, connexion Postgres, Flyway (V1),
`SecurityConfig` (chaîne JWT stateless + matchers public/authentifié), `JwtService` + filtre,
`StorageService` (écriture fichier hors base), gestion d'erreurs `@RestControllerAdvice`.
**À NE PAS faire** : aucune logique métier de membre/historique encore.
**Context pack** : socle de contexte uniquement.
**Definition of Done** : `mvn spring-boot:run` démarre, Flyway applique V1, `/actuator/health` = UP,
un endpoint protégé renvoie 401 sans token.

## Phase 1 — Noyau (Auth · Membres · Historisation)
**Objectif** : implémenter intégralement `specs/03_spec-fonctionnelle-noyau.md`.
**Dépend de** : Phase 0.
**Périmètre** : modules `auth/`, `membre/`, `historique/`, + repositories associés + DTO + tests.
**Context pack** : socle de contexte **+** `03_spec-fonctionnelle-noyau.md`.
**Definition of Done** : la DoD du noyau (tous les `CA-A/B/C`).
> C'est la fondation : ne pas démarrer une autre phase tant que celle-ci n'est pas verte.

## Phase 2 — Gouvernance & adhésions
**Objectif** : workflow d'adhésion + pilotage des mandats/rôles par le directeur.
**Dépend de** : Phase 1 (utilise `MembreService`, `HistoriqueService`).
**Périmètre** : `gouvernance/` — soumission publique d'adhésion (`POST /api/public/adhesions`),
consultation/décision par le directeur (accept → crée compte+membre via `auth`/`membre`, ouvre 1ʳᵉ
affiliation+statut via `historique` ; refus → motif), CRUD mandats, attribution/clôture de rôles
(délègue au moteur d'historisation).
**Entités** : `DemandeAdhesion`, `Mandat`, `HistoriqueRole`.
**Règles clés** : décision **réservée au directeur en mandat** (`@PreAuthorize` + mandat actif) ;
acceptation génère un **token d'invitation** (lien d'activation).
**Context pack** : socle **+** `03_...md` (pour les services réutilisés) **+** la fiche de ce module.
**DoD** : une adhésion acceptée aboutit à un compte activable ; un refus notifie + conserve la trace.

## Phase 3 — Module scientifique
> Le plus gros. Le découper en **4 sous-sessions** pour ne pas déborder.

- **3a — Équipes & événements** : CRUD équipe, événements (types, éditions, organisateurs n-n),
  archivage par édition. Entités : `Equipe`, `Evenement`.
- **3b — Publications** : ajout (membres/doctorants), co-auteurs ordonnés, classement par
  auteur/année/équipe. Entités : `Publication`, `PublicationAuteur`.
- **3c — Documents & PV** : upload/download/versionnement/recherche (par événement, type, date),
  réunions + dépôt de PV. Entités : `Document`, `Reunion`. Téléchargement **contrôlé par rôle**.
- **3d — Rapport annuel** : agrégation événements+publications+activités d'une année → génération
  PDF → archivage (`RapportAnnuel` + `Document`), déclenchement planifié + à la demande du directeur.
**Dépend de** : Phases 1-2.
**Context pack par sous-session** : socle **+** la fiche du sous-module **uniquement** (ne pas charger
les 4 d'un coup). Réutiliser `StorageService` (3c) et `MembreService`.
**DoD (exemples)** : 3b → classement renvoie les bonnes publications triées ; 3d → un rapport unique
par année, régénérable, agrégé à partir des périodes de validité.

## Phase 4 — Matériel & conventions
**Objectif** : suivi matériel + besoins + partenariats.
**Dépend de** : Phases 1-2.
**Périmètre** : `materiel/` (arrivages, distribution tracée, vue d'équité, besoins + validation
direction) ; `collaboration/` partie conventions (partenaires, conventions, archivage docs).
**Entités** : `Materiel`, `DistributionMateriel`, `BesoinMateriel`, `Partenaire`, `Convention`.
**Règle clé** : « distribution équitable » = **reporting** (qui a reçu / pas reçu), pas d'algorithme.
`quantite_disponible` cohérente (CHECK en base).
**Context pack** : socle **+** fiche du module.
**DoD** : une distribution décrémente le disponible et apparaît dans la vue d'équité ; un besoin suit
le cycle EN_ATTENTE→VALIDE/REFUSE.

## Phase 5 — Collaboration & transversal
**Objectif** : communication interne + services transverses + portail public.
**Dépend de** : toutes les précédentes (consomme leurs données).
**Périmètre** : `collaboration/` discussions+messages (générale/équipe/événement) ;
`transversal/` notifications (table + polling, via **événements applicatifs Spring/Observer**),
calendrier (endpoint événements sur intervalle), recherche globale (membres/documents/événements/
publications en requêtes JPA), **portail public** (présentation, équipes, activités, publications).
**Entités** : `Discussion`, `Message`, `Notification` (+ lecture des autres).
**Règle clé** : notifications émises par **les autres modules** via events ; ce module fournit le
listener + l'API de lecture/marquage-lu.
**Context pack** : socle **+** fiche du module **+** la liste des `TypeNotification` déjà câblés.
**DoD** : créer un événement / valider une adhésion / ajouter un document génère la notification
attendue ; la recherche globale retourne des résultats typés ; le portail public n'expose aucune
donnée confidentielle.

---

## Récapitulatif de l'ordre et des dépendances

| Phase | Module | Dépend de | Sessions |
|---|---|---|---|
| 0 | Socle technique | — | 1 |
| 1 | **Noyau** (auth/membre/historisation) | 0 | 1 |
| 2 | Gouvernance & adhésions | 1 | 1 |
| 3 | Scientifique (équipes/événements, publications, documents/PV, rapport) | 1-2 | 4 |
| 4 | Matériel & conventions | 1-2 | 1-2 |
| 5 | Collaboration & transversal (notif/calendrier/recherche/public) | toutes | 1-2 |

**Pourquoi cet ordre** : le noyau fige l'identité et le moteur temporel dont **tout** dépend ;
la gouvernance débloque la création de membres (adhésions) ; le scientifique est le cœur de valeur ;
matériel/conventions sont indépendants ; le transversal vient en dernier car il **consomme** les
événements émis par les modules précédents.

## Quand demander la suite
Les fiches détaillées (règles + `CA`) des phases 2 à 5 sont produites **à la demande**, une par une,
au format de `03_spec-fonctionnelle-noyau.md`. Demander la fiche d'une phase **juste avant** de la
construire garantit qu'elle reste alignée avec ce qui a réellement été codé dans les phases
précédentes.
