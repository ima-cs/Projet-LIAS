# LIAS — Package « AI-ready » pour le projet JEE

Ce dépôt contient tout ce qu'une IA (ou un développeur) doit avoir **figé à l'avance** pour
construire l'application de gestion du laboratoire LIAS **avec précision, sans inventer le modèle
ni mélanger les modules**. Il complète le document de conception (le *quoi*) par une couche
d'implémentation (le *comment exact*).

## Décisions verrouillées

| Sujet | Choix |
|---|---|
| Architecture | **React (SPA) + API REST**, sécurisée par **JWT** (stateless) |
| Build / langage | **Maven · Spring Boot 3.3 · Java 21** |
| Base de données | **PostgreSQL** (schéma géré par **Flyway**) |
| Persistance | Spring Data JPA / Hibernate + **Hibernate Envers** (audit profil & compte) |
| Sécurité | Spring Security (JWT) + **RBAC** dérivé du statut/rôle + BCrypt |
| Fichiers | Stockés **hors base** (chemin + métadonnées en base) |

## Hypothèses métier figées

1. **Pas d'auto-inscription** : le compte est créé à la **validation d'une adhésion** par le
   directeur ; le membre définit son mot de passe via un **lien d'invitation** (token).
2. **Appartenance à une équipe** = relation **temporelle historisée**, **une seule équipe active**
   à la fois (période ouverte = équipe actuelle).
3. **Rôles de gouvernance** portés par `historique_role` avec `mandat_id` ; `mandat` borne la
   période, sans dupliquer les FK directeur/vice/chef.
4. **Directeur / vice-directeur / chefs d'équipe = membres permanents** : la gouvernance est un
   **rôle**, jamais un statut. Le « permanent externe » reste de statut `PERMANENT` (origine portée
   par `etablissement_origine` / `laboratoire_origine`).
5. **Administrateur système** = marqueur technique `utilisateur.est_admin` (ni statut ni rôle).
6. **Droits des associés** : consultation + publications dont ils sont co-auteurs + participation
   aux événements ; **aucun** accès aux modules internes (matériel, PV, conventions, gouvernance).
7. **Édition de profil** : chacun édite **le sien** ; l'**admin** peut éditer tout compte ; le
   directeur **ne touche pas** aux profils (il gère adhésions/rôles).
8. **Distribution « équitable » du matériel** : le système **trace** qui a reçu / pas reçu et expose
   une **vue d'équité** — pas d'allocation automatique.
9. **Notifications** in-app (table + polling) ; e-mail non requis. **Recherche globale** en requêtes
   JPA multi-entités (pas de moteur externe).
10. **Règle d'or** : aucune suppression physique. Un changement = clôture d'une période + ouverture
    d'une nouvelle ; un départ/retraite = changement d'**état de compte**, jamais un `DELETE`.

## Contenu du package

```
00_README.md                  ← ce fichier (décisions, hypothèses, conventions)
specs/
  01_schema.sql               ← DDL PostgreSQL complet (copie de la migration Flyway V1)
  03_spec-fonctionnelle-noyau.md  ← spec détaillée : Auth · Membres · Historisation
  04_plan-construction.md     ← plan ordonné + « context pack » par phase
lias-backend/                 ← squelette Spring Boot (modèle figé)
  pom.xml
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__init_schema.sql
  src/main/java/ma/fsbm/lias/domain/        ← 24 entités JPA + enums (LA fondation)
    enums/   (12 enums)
    *.java   (entités annotées : types, relations, @Audited)
```

## Structure cible du backend (à respecter pour ne pas mélanger les modules)

```
ma.fsbm.lias
├── domain/              ← entités (FOURNI, stable ; modifiable seulement via nouvelle migration)
│   └── enums/
├── repository/          ← interfaces Spring Data (une par agrégat)
├── config/              ← SecurityConfig, JwtFilter, StorageConfig…
├── common/              ← exceptions, mappers, réponses d'erreur
└── module/              ← un sous-paquet par module, frontières étanches
    ├── auth/            (controller · service · dto · security)
    ├── membre/
    ├── historique/
    ├── gouvernance/     (mandats, rôles, adhésions)
    ├── science/         (evenement · publication · document · rapport)
    ├── materiel/
    ├── collaboration/   (discussions, conventions)
    └── transversal/     (notification · calendrier · recherche · portail public)
```

**Règle anti-mélange** : un module n'écrit **que** dans ses propres entités. Toute écriture sur une
entité partagée (ex. `Membre`) passe par le **service propriétaire** (`membre/MembreService`,
`historique/HistoriqueService`), jamais en accédant au repository d'un autre module. Les modules
communiquent par **interfaces de service**, pas en se partageant des repositories.

## Conventions transverses

- **Patron temporel** : toute entité d'historique porte `date_debut` / `date_fin` ; `date_fin IS NULL`
  = valeur courante. Des **index uniques partiels** garantissent une seule période ouverte pour
  l'affiliation, le statut et l'équipe (cf. `01_schema.sql`).
- **Enums** : `@Enumerated(EnumType.STRING)` côté JPA + `CHECK (... IN (...))` côté SQL (valeurs
  strictement alignées).
- **Audit** : `@Audited` uniquement sur `Membre` et `Utilisateur` (profil + compte). Les autres
  historiques sont **explicites** (tables dédiées), donc pas besoin d'Envers.
- **DTO obligatoires** : ne jamais exposer les entités JPA directement dans l'API (surtout pour
  masquer `date_naissance` et le hash). Mapping entité→DTO dans la couche service.

## Prérequis & démarrage

```bash
# PostgreSQL local
createdb lias && psql -c "CREATE USER lias WITH PASSWORD 'lias'; GRANT ALL ON DATABASE lias TO lias;"
# Backend
cd lias-backend
./mvnw spring-boot:run     # Flyway applique V1 au démarrage
```

> Le schéma est piloté par **Flyway** (`ddl-auto: validate`). Hibernate ne crée rien : toute
> évolution du modèle = **nouvelle migration** `V2__...sql`, jamais une modification de V1.

Pour la suite de la construction, suivre **`specs/04_plan-construction.md`** (une phase = une session).
