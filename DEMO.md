# Présenter LIAS

Application locale : React SPA, API Spring Boot 3.3 / Java 21, PostgreSQL, Flyway et Envers.

## Démarrage en deux terminaux PowerShell

Prérequis : JDK 21, Maven 3.9+, Node.js 22.12+ (Node 24 fonctionne), PostgreSQL 17. Le service `postgresql-x64-17` et la base `lias` existent sur cette machine. Les variables `LIAS_DB_PASSWORD` et `LIAS_JWT_SECRET` étaient déjà configurées dans l’environnement utilisé pour le développement. Aucun secret réel n’est enregistré dans Git.

Depuis la racine du dépôt, terminal 1 :

```powershell
# Si PostgreSQL est arrêté, dans un terminal administrateur :
Start-Service postgresql-x64-17

# Définir seulement si la variable n’est pas déjà présente :
# $env:LIAS_DB_PASSWORD = 'votre mot de passe PostgreSQL du rôle lias'
.\start-backend.ps1
```

Le script génère une clé JWT aléatoire pour le processus si aucune clé n’est définie. Un changement de clé nécessite de se reconnecter. Le profil `demo` ajoute les données manquantes sans recréer les comptes existants ni réinitialiser leurs mots de passe. Hibernate reste en `validate` ; Flyway applique V1 puis V2.

Terminal 2 :

```powershell
.\start-frontend.ps1
```

- Interface : **http://127.0.0.1:5173/**
- API : **http://localhost:8080/api/**
- Santé : **http://localhost:8080/actuator/health** → `{"status":"UP"}`

Les appels `/api` de React passent par le proxy Vite vers Spring Boot. Les données affichées proviennent de PostgreSQL.

## Comptes de démonstration

Mot de passe commun initial : **`LiasDemo2026!`** (stocké haché BCrypt).

| E-mail | Fonction |
|---|---|
| `directeur@lias.ma` | Permanent, directeur dans le mandat actif |
| `permanent@lias.ma` | Membre permanent |
| `doctorant@lias.ma` | Doctorant |
| `associe@lias.ma` | Associé |
| `admin@lias.ma` | Administrateur technique, sans droits de directeur |

Le bouton **Espace membre** ouvre la connexion. La déconnexion est en bas de la barre latérale. Les tokens sont conservés dans `sessionStorage` pendant l’onglet et expirent après 30 minutes. Les autorités et l’état du compte sont relus côté serveur à chaque requête.

## Parcours conseillé (10–15 minutes)

1. **Visiteur** : accueil → Équipes → Chercheurs → fiche publique → Événements → Publications. Montrer l’absence de date de naissance. Dans **Nous rejoindre**, déposer une demande avec une adresse nouvelle, éventuellement un CV PDF.
2. **Directeur** : connexion → dashboard → **Adhésions** → Examiner → Accepter, choisir le statut et éventuellement une équipe. La ligne passe à `ACCEPTEE` et le membre apparaît dans l’annuaire. Une autre demande peut être refusée avec un motif obligatoire.
3. **Activation sans e-mail** : après acceptation, l’écran du directeur affiche **Compte créé : transmettre le lien d’activation**, avec **Copier le lien** et **Ouvrir la page d’activation**. Transmettre ce lien au candidat, qui choisit et confirme son mot de passe, puis se connecte avec son e-mail. Après un rechargement, le bouton **Lien d’activation** de la candidature acceptée permet de retrouver l’invitation tant qu’elle reste valide. Le lien disparaît après activation ou expiration. Le token est à usage unique, valable 72 heures. Cette remise visuelle est réservée au profil `demo` et au directeur actif ; aucune liste publique ou fiche membre ne l’expose. Le fichier local `lias-backend/storage/invitations-demo.txt` reste une copie de secours ignorée par Git. Hors profil demo, brancher un canal privé de remise d’invitation sur `InvitationCreated`.
4. **Directeur, activité scientifique** : Événements → créer un séminaire, ajouter plusieurs organisateurs avec leurs identifiants (référence affichée sous le formulaire). Publications → ajouter un article avec les identifiants des auteurs dans leur ordre de signature. Consulter le calendrier.
5. **Fichiers et pilotage** : Documents → déposer un PDF → Télécharger → Nouvelle version. Réunions & PV → consulter le PV existant. Inventaire → voir les stocks → Distribuer → Vue d’équité. Besoins matériels → Valider ou Refuser une demande. Partenaires et Conventions → consulter ou modifier les accords et télécharger leur archive.
6. **Rapport annuel** : sélectionner 2026 → Générer → Télécharger PDF. Régénérer : même rapport annuel, nouvelle version documentaire. Consulter les notifications.
7. **Permanent** : se déconnecter, se connecter comme permanent → Mon profil → Modifier → Enregistrer → Historique des modifications. Annuaire → fiche d’un collègue → reconstituer son état à une date. Besoins matériels → demander un équipement. Discussions → Vie du laboratoire → poster un message.
8. **Historisation, admin** : se connecter comme admin → Historisation → choisir le membre de démonstration voulu → changer son statut ou son équipe. Montrer les anciennes périodes clôturées et la nouvelle période ouverte, puis la reconstitution à une date. Un départ désactive le compte ; un retour ouvre une affiliation et réactive le compte. Une retraite gèle le compte. Ne pas modifier le directeur pendant la partie scientifique de la démonstration.
9. **Fin de mandat, en dernier** : admin → Historisation → Clôturer le mandat. Le directeur perd immédiatement ses droits de décision, même avec un JWT déjà émis. Le rôle historique reste conservé. Cette action est persistante ; le seed ne réouvre pas un mandat fermé. Pour une nouvelle démonstration, créer un nouveau mandat avec l’admin ; l’attribution d’un nouveau directeur nécessite ensuite une opération administrative encadrée en base ou l’existence d’un autre directeur actif. Garder cette démonstration pour la fin.

## Vérification et relance

```powershell
cd lias-backend
mvn clean test
# Tests HTTP/intégration sur le schéma PostgreSQL séparé lias_test.
# Les modifications des tests sont annulées par transaction.

cd ..\lias-frontend
npm.cmd ci
npm.cmd run build
npm.cmd exec playwright test
# Les tests navigateur utilisent Edge installé sur cette machine.
# Backend et Vite doivent être démarrés. Ils créent quelques traces de démonstration.
```

Pour relancer : `Ctrl+C` dans le terminal concerné, puis le script correspondant depuis la racine. Attendre le message `Started LiasApplication` avant d’ouvrir la page. Après une mise à jour des dépendances npm, relancer Vite. Si un port est occupé, retrouver le terminal existant plutôt que démarrer une deuxième instance.

Sur une machine neuve : créer le rôle PostgreSQL `lias` et la base `lias` dont il est propriétaire, définir `LIAS_DB_PASSWORD`, puis lancer les scripts. Pour changer de base, utiliser `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` et `SPRING_DATASOURCE_PASSWORD`.

Les fichiers déposés sont dans `lias-backend/storage/`, avec un nom interne aléatoire. Ils ne doivent pas être déplacés pendant la démonstration. Les téléchargements passent par une API sécurisée. Le profil test utilise `target/test-storage/`.

## Choix techniques utiles à expliquer

- Aucune modification de `specs/01_schema.sql`, V1 ou des entités du modèle initial. V2 ajoute uniquement les contraintes d’unicité sur les e-mails normalisés, les demandes en attente et les versions documentaires concurrentes.
- V3 impose l’unicité des tokens d’activation non nuls. Le modèle métier reste inchangé ; les invitations utilisent 256 bits aléatoires et sont consommées sous verrou lors de l’activation.
- Les changements de statut, affiliation, équipe et rôle conservent les périodes précédentes. Envers historise les modifications de profil et de compte.
- L’administrateur est un marqueur technique. Les droits métier sont dérivés du statut et des rôles dans un mandat actif.
- Notifications persistées à partir d’événements Spring, lecture personnelle et polling React. Aucun service e-mail nécessaire à la démonstration.
- PDF produit localement, paginé et archivé comme Document ; un rapport par année, plusieurs versions possibles.
- Rapport planifiable via `LIAS_REPORT_CRON`, désactivé par défaut (`-`). Exemple : `0 0 8 2 1 *` pour le 2 janvier à 8h, fuseau Casablanca ; génération de l’année précédente au nom du directeur actif.

## Limites de portée

Le modèle officiel ne définit pas d’inscription de participants à un événement : les discussions d’événement sont accessibles aux permanents et aux organisateurs explicitement liés. Les associés peuvent consulter les événements et contribuer aux publications dont ils sont auteurs. Les notifications de refus sont conservées côté direction : un candidat refusé ne dispose pas d’un compte interne. Le rapport couvre les données scientifiques et périodes présentes en base ; il ne fabrique pas d’activité absente.

## État de la démonstration préparée sur cette machine

Le membre **Youssef El Amrani** possède déjà trois périodes de statut, préparées par l’API d’administration : `PERMANENT` du 01/01/2020 au 01/01/2022, `ASSOCIE` jusqu’au 01/01/2024, puis `PERMANENT` depuis le 01/01/2024. Sa fiche à la date du **01/06/2022** montre donc `ASSOCIE`, alors que sa fiche actuelle montre `PERMANENT`. Les trois lignes sont visibles dans Historisation et conservées lors des redémarrages.

Les tests navigateur laissent volontairement quelques traces réelles : candidatures, un compte activé supplémentaire, messages, besoins, événements et publications de démonstration. Les cinq comptes principaux restent disponibles et une candidature initiale reste en attente.
