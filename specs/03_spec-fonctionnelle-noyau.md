# Spécification fonctionnelle — NOYAU

Périmètre : **Authentification & comptes**, **Membres & profils**, **Historisation**.
C'est la fondation dont dépendent tous les autres modules : à construire **en premier**, et à figer
avant d'attaquer le reste.

Conventions de lecture : `RG` = règle de gestion (invariant à respecter), `CA` = critère
d'acceptation (vérifiable par un test). Tous les endpoints renvoient des **DTO**, jamais les entités.
Codes d'erreur normalisés : `400` (validation), `401` (non authentifié), `403` (interdit),
`404` (introuvable), `409` (conflit d'état/règle métier).

---

## Module A — Authentification & comptes

### A.1 Périmètre
- Connexion par e-mail + mot de passe, émission d'un **JWT** d'accès.
- **Activation de compte** via lien d'invitation (définition du mot de passe).
- Récupération du profil courant + autorités.
- Gestion de l'**état du compte** par l'admin (geler / désactiver / réactiver).
- **Dérivation des autorités** (RBAC) à partir du statut, du rôle de gouvernance et de `est_admin`.

### A.2 Entités concernées
`Utilisateur` (écriture), `Membre`, `HistoriqueStatut`, `HistoriqueRole`, `Mandat` (lecture, pour
dériver les autorités). Aucune autre entité n'est modifiée par ce module.

### A.3 Règles de gestion
- **RG-A1** Seul un compte `etat_compte = ACTIF` peut se connecter. `GELE` → 403 « compte gelé » ;
  `DESACTIVE` → 403 « compte désactivé ».
- **RG-A2** Le mot de passe est stocké **haché BCrypt** ; jamais renvoyé ni journalisé.
- **RG-A3** À la connexion réussie, mettre à jour `derniere_connexion` et émettre un JWT signé
  contenant : `sub` = email, `membreId`, et les **autorités** (cf. RG-A6). TTL = 30 min.
- **RG-A4** L'**activation** : le compte créé par acceptation d'adhésion est `ACTIF` mais sans mot de
  passe, avec `token_activation` + `token_expiration`. L'endpoint d'activation valide le token
  (non expiré), pose le hash BCrypt, **efface** le token. Token invalide/expiré → 400.
- **RG-A5** La gestion d'état est réservée à `ROLE_ADMIN` :
  retraite → `GELE` ; départ → `DESACTIVE` ; réactivation → `ACTIF`. Ces transitions n'effacent
  aucune donnée (cf. RG d'historisation, module C).
- **RG-A6** **Dérivation des autorités** (à recalculer à chaque émission de token / appel `/me`) :
  - autorité de base depuis le **statut courant** (`historique_statut` ouvert) :
    `PERMANENT→ROLE_PERMANENT`, `ASSOCIE→ROLE_ASSOCIE`, `DOCTORANT→ROLE_DOCTORANT` ;
  - `+ ROLE_DIRECTEUR` **si et seulement si** le membre a un `historique_role` ouvert de type
    `DIRECTEUR` rattaché à un **mandat actif** (`mandat.date_fin IS NULL`) ;
  - `+ ROLE_ADMIN` si `utilisateur.est_admin = true` ;
  - statuts `RETRAITE`/`ANCIEN` → aucune autorité métier (de toute façon non connectables, RG-A1).
- **RG-A7** Le rôle `ROLE_DIRECTEUR` **expire automatiquement** : dès que le mandat est clôturé, la
  dérivation cesse de l'accorder (aucune action de révocation manuelle nécessaire).

### A.4 Endpoints
| Méthode | Chemin | Auth | Corps / params | Réponse |
|---|---|---|---|---|
| POST | `/api/auth/login` | public | `{email, motDePasse}` | `{accessToken, expiresIn, profil}` |
| POST | `/api/auth/activer` | public | `{token, motDePasse}` | `204` |
| GET | `/api/auth/me` | authentifié | — | `{profil, autorites[]}` |
| PATCH | `/api/admin/comptes/{utilisateurId}/etat` | `ROLE_ADMIN` | `{etat}` | `{compte}` |

### A.5 Critères d'acceptation
- **CA-A1** Login avec identifiants valides sur compte `ACTIF` → 200 + token exploitable sur `/me`.
- **CA-A2** Login sur compte `GELE` ou `DESACTIVE` → 403 avec message explicite, **aucun** token émis.
- **CA-A3** Login avec mauvais mot de passe → 401 ; le message ne révèle pas si l'e-mail existe.
- **CA-A4** Activation avec token valide → le membre peut ensuite se connecter ; le token est
  inutilisable une seconde fois (400).
- **CA-A5** Un membre dont le statut courant est `PERMANENT` **et** ayant un rôle `DIRECTEUR` ouvert
  dans un mandat actif obtient `[ROLE_PERMANENT, ROLE_DIRECTEUR]` sur `/me`.
- **CA-A6** Après clôture du mandat, le même appel `/me` ne renvoie plus `ROLE_DIRECTEUR` sans
  qu'aucune donnée n'ait été supprimée.
- **CA-A7** Un non-admin appelant `/api/admin/comptes/...` → 403.

---

## Module B — Membres & profils

### B.1 Périmètre
- Consultation des profils (interne) et fiche **publique** restreinte.
- Édition de **son** profil (et de tout profil pour l'admin).
- Listing/recherche des membres (pagination, filtres statut/équipe).
- Photo de profil (upload). Historique des modifications de profil (via Envers).

### B.2 Entités concernées
`Membre` (écriture), `Utilisateur` (lecture), `membre_centre_interet`. Les valeurs **dérivées**
(statut courant, équipe actuelle, rôles) sont **lues** auprès du module Historisation, jamais
recalculées ici.

### B.3 Règles de gestion
- **RG-B1** Un membre ne peut modifier **que son propre** profil ; `ROLE_ADMIN` peut modifier tout
  profil ; toute autre tentative → 403.
- **RG-B2** `date_naissance` est **confidentielle** : exclue de la fiche publique et de toute réponse
  destinée à un autre membre ; visible seulement par le membre lui-même et l'admin.
- **RG-B3** Toute modification de profil est **historisée automatiquement** (Envers, `membre_aud`).
  Aucune action explicite n'est requise côté code métier.
- **RG-B4** Champs **non éditables** par le membre via ce module : `utilisateur`, statut, rôle,
  équipe, affiliations (ils relèvent du module Historisation / gouvernance).
- **RG-B5** L'upload de photo respecte les contraintes de stockage (type/MIME, taille max) ; le
  binaire va **hors base**, `photo_chemin` enregistre le chemin.

### B.4 Endpoints
| Méthode | Chemin | Auth | Corps / params | Réponse |
|---|---|---|---|---|
| GET | `/api/membres` | authentifié | `?page&size&statut&equipeId&q` | page de `MembreResume` |
| GET | `/api/membres/{id}` | authentifié | — | `MembreDetail` (sans `date_naissance` si autrui) |
| GET | `/api/public/membres/{id}` | public | — | `MembrePublic` (restreint) |
| PUT | `/api/membres/{id}` | self/admin | `MembreUpdate` | `MembreDetail` |
| PUT | `/api/membres/{id}/photo` | self/admin | `multipart file` | `{photoUrl}` |
| GET | `/api/membres/{id}/historique` | self/admin | — | révisions Envers (profil) |

### B.5 Critères d'acceptation
- **CA-B1** Un membre édite son profil → 200 ; une révision apparaît dans `/historique`.
- **CA-B2** Un membre tente d'éditer le profil d'un autre → 403.
- **CA-B3** La fiche publique et la fiche « vue par autrui » ne contiennent **jamais**
  `date_naissance` ; la fiche « vue par soi-même » la contient.
- **CA-B4** `GET /api/membres?statut=PERMANENT&equipeId=3` ne renvoie que des membres dont le
  **statut courant** est PERMANENT et l'**équipe actuelle** est l'équipe 3.
- **CA-B5** Upload d'un fichier non autorisé (ex. `.exe` ou > 20 Mo) → 400, aucun fichier écrit.

---

## Module C — Historisation (le moteur temporel)

### C.1 Périmètre
Services et endpoints qui **ouvrent / clôturent** les périodes (affiliation, statut, équipe, rôle),
exposent les valeurs **courantes** et permettent de **reconstituer l'état** d'un membre à une date.
C'est le module le plus structurant : tous les autres en dépendent pour « qui est quoi, quand ».

### C.2 Entités concernées
`Affiliation`, `HistoriqueStatut`, `AppartenanceEquipe`, `HistoriqueRole`, `Mandat` (écriture).
`Membre` (lecture seule).

### C.3 Règles de gestion
- **RG-C1** **Jamais de suppression** : changer une valeur = **clôturer** la période ouverte
  (`date_fin = date du changement`) puis **ouvrir** une nouvelle période.
- **RG-C2** **Une seule période ouverte** à la fois pour l'affiliation, le statut et l'équipe (garanti
  aussi par index unique partiel). Tenter d'en ouvrir une seconde → 409.
- **RG-C3** Les **rôles peuvent se cumuler** (ex. `MEMBRE_EQUIPE` + `CHEF_EQUIPE`) : pas d'unicité sur
  la période ouverte des rôles.
- **RG-C4** Un rôle de **gouvernance** (`DIRECTEUR`, `VICE_DIRECTEUR`, `CHEF_EQUIPE`) ne peut être
  ouvert que pour un membre dont le **statut courant est `PERMANENT`** et avec un `mandat_id`
  référençant un **mandat actif** ; sinon 409.
- **RG-C5** `CHEF_EQUIPE` et `MEMBRE_EQUIPE` exigent un `equipe_id` ; `DIRECTEUR` et `VICE_DIRECTEUR`
  ont `equipe_id = null`. Violation → 400.
- **RG-C6** Cohérence des bornes : `date_fin >= date_debut` ; une nouvelle période commence à une date
  `>=` à la fin de la précédente. Violation → 400.
- **RG-C7** Transitions de cycle de vie standard (orchestrées avec le module A) :
  *retraite* → clôt l'affiliation + ouvre statut `RETRAITE` + compte `GELE` ;
  *départ* → clôt l'affiliation + ouvre statut `ANCIEN` + compte `DESACTIVE` ;
  *retour* → ouvre une **nouvelle** affiliation + nouveau statut, compte réactivé.
- **RG-C8** **Reconstitution** à une date `d` : la valeur courante à `d` est la période telle que
  `date_debut <= d AND (date_fin IS NULL OR date_fin > d)`.

### C.4 Endpoints (réservés `ROLE_ADMIN` sauf mention ; rôles : `ROLE_DIRECTEUR`)
| Méthode | Chemin | Auth | Corps | Effet |
|---|---|---|---|---|
| POST | `/api/membres/{id}/statut` | admin | `{type, dateDebut}` | clôt + ouvre statut |
| POST | `/api/membres/{id}/affiliation` | admin | `{action: DEPART/RETOUR, date}` | clôt ou ouvre affiliation |
| POST | `/api/membres/{id}/equipe` | admin/directeur | `{equipeId, dateDebut}` | change d'équipe (clôt l'ancienne) |
| POST | `/api/membres/{id}/roles` | directeur | `{type, equipeId?, mandatId, dateDebut}` | ouvre un rôle |
| PATCH | `/api/membres/{id}/roles/{roleId}/cloturer` | directeur | `{dateFin}` | clôt un rôle |
| GET | `/api/membres/{id}/etat` | authentifié | `?date=YYYY-MM-DD` | état reconstitué (statut, équipe, rôles) |
| POST | `/api/mandats` | admin | `{libelle, dateDebut}` | crée un mandat |
| PATCH | `/api/mandats/{id}/cloturer` | admin | `{dateFin}` | clôt le mandat (les rôles `DIRECTEUR` cessent d'accorder l'autorité) |

### C.5 Critères d'acceptation
- **CA-C1** Changer le statut d'un membre crée **deux** lignes (l'ancienne clôturée, la nouvelle
  ouverte) ; aucune ligne supprimée.
- **CA-C2** Ouvrir une seconde affiliation ouverte → 409 ; l'index partiel empêche aussi l'insertion
  directe.
- **CA-C3** Un membre quitte (départ) puis revient (retour) → `historique_statut` et `affiliation`
  contiennent l'historique complet des deux passages, le compte est réactivé au retour.
- **CA-C4** Ouvrir un rôle `DIRECTEUR` pour un membre `DOCTORANT` → 409 (RG-C4).
- **CA-C5** Ouvrir `CHEF_EQUIPE` sans `equipeId` → 400 (RG-C5).
- **CA-C6** `GET /api/membres/{id}/etat?date=2021-06-01` renvoie l'équipe, le statut et les rôles
  **tels qu'ils étaient** à cette date (vérifiable sur un jeu de données avec changements).
- **CA-C7** Après `cloturer` d'un mandat, un membre qui était directeur conserve sa ligne de rôle
  (clôturée) et perd l'autorité `ROLE_DIRECTEUR` (lien avec CA-A6).

---

## Definition of Done du noyau
1. Tous les `CA-*` ci-dessus passent en tests d'intégration (`@SpringBootTest` + base de test).
2. La couche sécurité applique la matrice : public / authentifié / `ROLE_ADMIN` / `ROLE_DIRECTEUR`.
3. Aucune entité JPA n'est exposée hors des DTO ; `date_naissance` et le hash ne fuient jamais.
4. Les invariants temporels (une seule période ouverte) sont garantis **en base** (index partiels)
   **et** en service (409 explicite).
5. La modification de profil produit bien une révision Envers consultable.
