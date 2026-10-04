-- =====================================================================
--  LIAS — Schéma initial (PostgreSQL)  ·  migration Flyway V1
--  Conventions :
--   * PK : BIGINT GENERATED ALWAYS AS IDENTITY
--   * enums : VARCHAR + CHECK (alignés sur les enum Java, EnumType.STRING)
--   * temporel métier : DATE (date_debut / date_fin, NULL = période ouverte)
--   * temporel technique : TIMESTAMP
--   * « aucune suppression » : pas d'ON DELETE CASCADE sur les liens métier
-- =====================================================================

-- ---------- Institutionnel ----------
CREATE TABLE laboratoire (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom                VARCHAR(200) NOT NULL,
    sigle              VARCHAR(30),
    date_creation_labo DATE,
    presentation       TEXT
);

-- ---------- Identité / compte ----------
CREATE TABLE utilisateur (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email             VARCHAR(180) NOT NULL,
    mot_de_passe_hash VARCHAR(100),
    etat_compte       VARCHAR(20)  NOT NULL DEFAULT 'ACTIF',
    est_admin         BOOLEAN      NOT NULL DEFAULT FALSE,
    token_activation  VARCHAR(120),
    token_expiration  TIMESTAMP,
    date_creation     TIMESTAMP    NOT NULL DEFAULT now(),
    derniere_connexion TIMESTAMP,
    CONSTRAINT uk_utilisateur_email UNIQUE (email),
    CONSTRAINT ck_utilisateur_etat  CHECK (etat_compte IN ('ACTIF','GELE','DESACTIVE'))
);

CREATE TABLE equipe (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom         VARCHAR(120) NOT NULL,
    thematique  VARCHAR(200),
    description TEXT,
    CONSTRAINT uk_equipe_nom UNIQUE (nom)
);

CREATE TABLE mandat (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    libelle    VARCHAR(120),
    date_debut DATE NOT NULL,
    date_fin   DATE,
    CONSTRAINT ck_mandat_periode CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE TABLE membre (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    utilisateur_id       BIGINT NOT NULL,
    nom                  VARCHAR(80) NOT NULL,
    prenom               VARCHAR(80) NOT NULL,
    photo_chemin         VARCHAR(255),
    biographie           TEXT,
    date_naissance       DATE,
    date_embauche        DATE,
    etablissement_origine VARCHAR(160),
    laboratoire_origine  VARCHAR(160),
    CONSTRAINT uk_membre_utilisateur UNIQUE (utilisateur_id),
    CONSTRAINT fk_membre_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id)
);

CREATE TABLE membre_centre_interet (
    membre_id      BIGINT NOT NULL,
    centre_interet VARCHAR(120) NOT NULL,
    CONSTRAINT pk_membre_centre PRIMARY KEY (membre_id, centre_interet),
    CONSTRAINT fk_centre_membre FOREIGN KEY (membre_id) REFERENCES membre (id)
);

-- ---------- Historique temporel ----------
CREATE TABLE affiliation (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    membre_id  BIGINT NOT NULL,
    date_debut DATE NOT NULL,
    date_fin   DATE,
    CONSTRAINT fk_affiliation_membre FOREIGN KEY (membre_id) REFERENCES membre (id),
    CONSTRAINT ck_affiliation_periode CHECK (date_fin IS NULL OR date_fin >= date_debut)
);
-- Au plus UNE affiliation ouverte par membre
CREATE UNIQUE INDEX uidx_affiliation_active ON affiliation (membre_id) WHERE date_fin IS NULL;

CREATE TABLE historique_statut (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    membre_id   BIGINT NOT NULL,
    type_statut VARCHAR(20) NOT NULL,
    date_debut  DATE NOT NULL,
    date_fin    DATE,
    CONSTRAINT fk_statut_membre FOREIGN KEY (membre_id) REFERENCES membre (id),
    CONSTRAINT ck_statut_type CHECK (type_statut IN ('PERMANENT','ASSOCIE','DOCTORANT','RETRAITE','ANCIEN')),
    CONSTRAINT ck_statut_periode CHECK (date_fin IS NULL OR date_fin >= date_debut)
);
CREATE UNIQUE INDEX uidx_statut_courant ON historique_statut (membre_id) WHERE date_fin IS NULL;

CREATE TABLE appartenance_equipe (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    membre_id  BIGINT NOT NULL,
    equipe_id  BIGINT NOT NULL,
    date_debut DATE NOT NULL,
    date_fin   DATE,
    CONSTRAINT fk_appartenance_membre FOREIGN KEY (membre_id) REFERENCES membre (id),
    CONSTRAINT fk_appartenance_equipe FOREIGN KEY (equipe_id) REFERENCES equipe (id),
    CONSTRAINT ck_appartenance_periode CHECK (date_fin IS NULL OR date_fin >= date_debut)
);
-- Une seule équipe active à la fois
CREATE UNIQUE INDEX uidx_appartenance_active ON appartenance_equipe (membre_id) WHERE date_fin IS NULL;

CREATE TABLE historique_role (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    membre_id  BIGINT NOT NULL,
    type_role  VARCHAR(20) NOT NULL,
    equipe_id  BIGINT,
    mandat_id  BIGINT,
    date_debut DATE NOT NULL,
    date_fin   DATE,
    CONSTRAINT fk_role_membre FOREIGN KEY (membre_id) REFERENCES membre (id),
    CONSTRAINT fk_role_equipe FOREIGN KEY (equipe_id) REFERENCES equipe (id),
    CONSTRAINT fk_role_mandat FOREIGN KEY (mandat_id) REFERENCES mandat (id),
    CONSTRAINT ck_role_type CHECK (type_role IN ('DIRECTEUR','VICE_DIRECTEUR','CHEF_EQUIPE','MEMBRE_EQUIPE')),
    CONSTRAINT ck_role_periode CHECK (date_fin IS NULL OR date_fin >= date_debut)
);
CREATE INDEX idx_role_membre_actif ON historique_role (membre_id) WHERE date_fin IS NULL;

-- ---------- Activité scientifique ----------
CREATE TABLE evenement (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type_evenement     VARCHAR(20) NOT NULL,
    titre              VARCHAR(200) NOT NULL,
    description        TEXT,
    edition            VARCHAR(60),
    date_debut         DATE NOT NULL,
    date_fin           DATE,
    lieu               VARCHAR(200),
    cree_par_membre_id BIGINT NOT NULL,
    date_creation      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_evenement_createur FOREIGN KEY (cree_par_membre_id) REFERENCES membre (id),
    CONSTRAINT ck_evenement_type CHECK (type_evenement IN ('CONFERENCE','SEMINAIRE','WORKSHOP')),
    CONSTRAINT ck_evenement_periode CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE TABLE document (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type_document         VARCHAR(30) NOT NULL,
    nom_fichier           VARCHAR(255) NOT NULL,
    chemin_stockage       VARCHAR(500) NOT NULL,
    type_mime             VARCHAR(100),
    taille_octets         BIGINT,
    version               INT NOT NULL DEFAULT 1,
    document_parent_id    BIGINT,
    evenement_id          BIGINT,
    uploaded_by_membre_id BIGINT NOT NULL,
    date_upload           TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_document_parent    FOREIGN KEY (document_parent_id)    REFERENCES document (id),
    CONSTRAINT fk_document_evenement FOREIGN KEY (evenement_id)          REFERENCES evenement (id),
    CONSTRAINT fk_document_uploader  FOREIGN KEY (uploaded_by_membre_id) REFERENCES membre (id),
    CONSTRAINT ck_document_type CHECK (type_document IN
        ('DEMANDE_FINANCEMENT','PROGRAMME','ATTESTATION','RAPPORT','ADMINISTRATIF','PV','CV','AUTRE'))
);
CREATE INDEX idx_document_evenement ON document (evenement_id);
CREATE INDEX idx_document_type ON document (type_document);

CREATE TABLE demande_adhesion (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom                 VARCHAR(80) NOT NULL,
    prenom              VARCHAR(80) NOT NULL,
    email               VARCHAR(180) NOT NULL,
    motivation          TEXT,
    cv_document_id      BIGINT,
    statut_demande      VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    motif_refus         TEXT,
    date_soumission     TIMESTAMP NOT NULL DEFAULT now(),
    date_decision       TIMESTAMP,
    traite_par_membre_id BIGINT,
    membre_cree_id      BIGINT,
    CONSTRAINT fk_demande_cv      FOREIGN KEY (cv_document_id)       REFERENCES document (id),
    CONSTRAINT fk_demande_traite  FOREIGN KEY (traite_par_membre_id) REFERENCES membre (id),
    CONSTRAINT fk_demande_membre  FOREIGN KEY (membre_cree_id)       REFERENCES membre (id),
    CONSTRAINT ck_demande_statut CHECK (statut_demande IN ('EN_ATTENTE','ACCEPTEE','REFUSEE'))
);

CREATE TABLE publication (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titre                VARCHAR(300) NOT NULL,
    type_publication     VARCHAR(30) NOT NULL,
    annee                INT NOT NULL,
    doi                  VARCHAR(120),
    lien                 VARCHAR(300),
    equipe_id            BIGINT,
    ajoute_par_membre_id BIGINT NOT NULL,
    date_ajout           TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_publication_equipe FOREIGN KEY (equipe_id)            REFERENCES equipe (id),
    CONSTRAINT fk_publication_ajout  FOREIGN KEY (ajoute_par_membre_id) REFERENCES membre (id),
    CONSTRAINT ck_publication_type CHECK (type_publication IN
        ('ARTICLE_JOURNAL','ARTICLE_CONFERENCE','CHAPITRE_LIVRE','THESE','RAPPORT_TECHNIQUE','AUTRE'))
);
CREATE INDEX idx_publication_annee ON publication (annee);
CREATE INDEX idx_publication_equipe ON publication (equipe_id);

CREATE TABLE publication_auteur (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    publication_id BIGINT NOT NULL,
    membre_id      BIGINT NOT NULL,
    ordre_auteur   INT NOT NULL,
    CONSTRAINT fk_pa_publication FOREIGN KEY (publication_id) REFERENCES publication (id),
    CONSTRAINT fk_pa_membre      FOREIGN KEY (membre_id)      REFERENCES membre (id),
    CONSTRAINT uk_pub_auteur UNIQUE (publication_id, membre_id)
);

CREATE TABLE evenement_organisateur (
    evenement_id BIGINT NOT NULL,
    membre_id    BIGINT NOT NULL,
    CONSTRAINT pk_evt_org PRIMARY KEY (evenement_id, membre_id),
    CONSTRAINT fk_eo_evenement FOREIGN KEY (evenement_id) REFERENCES evenement (id),
    CONSTRAINT fk_eo_membre    FOREIGN KEY (membre_id)    REFERENCES membre (id)
);

CREATE TABLE reunion (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titre              VARCHAR(200) NOT NULL,
    date_reunion       DATE NOT NULL,
    ordre_du_jour      TEXT,
    pv_document_id     BIGINT,
    cree_par_membre_id BIGINT NOT NULL,
    CONSTRAINT fk_reunion_pv      FOREIGN KEY (pv_document_id)     REFERENCES document (id),
    CONSTRAINT fk_reunion_createur FOREIGN KEY (cree_par_membre_id) REFERENCES membre (id)
);

CREATE TABLE rapport_annuel (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    annee                INT NOT NULL,
    date_generation      TIMESTAMP NOT NULL DEFAULT now(),
    document_id          BIGINT,
    genere_par_membre_id BIGINT NOT NULL,
    CONSTRAINT uk_rapport_annee UNIQUE (annee),
    CONSTRAINT fk_rapport_document FOREIGN KEY (document_id)          REFERENCES document (id),
    CONSTRAINT fk_rapport_generateur FOREIGN KEY (genere_par_membre_id) REFERENCES membre (id)
);

-- ---------- Matériel ----------
CREATE TABLE materiel (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    designation         VARCHAR(200) NOT NULL,
    description         TEXT,
    quantite_initiale   INT NOT NULL,
    quantite_disponible INT NOT NULL,
    date_arrivage       DATE NOT NULL,
    CONSTRAINT ck_materiel_qte CHECK (quantite_disponible >= 0 AND quantite_disponible <= quantite_initiale)
);

CREATE TABLE distribution_materiel (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    materiel_id            BIGINT NOT NULL,
    membre_id              BIGINT NOT NULL,
    quantite               INT NOT NULL,
    date_distribution      DATE NOT NULL,
    distribue_par_membre_id BIGINT NOT NULL,
    CONSTRAINT fk_distrib_materiel FOREIGN KEY (materiel_id)            REFERENCES materiel (id),
    CONSTRAINT fk_distrib_membre   FOREIGN KEY (membre_id)              REFERENCES membre (id),
    CONSTRAINT fk_distrib_par      FOREIGN KEY (distribue_par_membre_id) REFERENCES membre (id),
    CONSTRAINT ck_distrib_qte CHECK (quantite > 0)
);

CREATE TABLE besoin_materiel (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    demandeur_membre_id BIGINT NOT NULL,
    designation         VARCHAR(200) NOT NULL,
    justification       TEXT,
    quantite_demandee   INT NOT NULL,
    statut_besoin       VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    date_demande        TIMESTAMP NOT NULL DEFAULT now(),
    date_decision       TIMESTAMP,
    traite_par_membre_id BIGINT,
    CONSTRAINT fk_besoin_demandeur FOREIGN KEY (demandeur_membre_id) REFERENCES membre (id),
    CONSTRAINT fk_besoin_traite    FOREIGN KEY (traite_par_membre_id) REFERENCES membre (id),
    CONSTRAINT ck_besoin_statut CHECK (statut_besoin IN ('EN_ATTENTE','VALIDE','REFUSE')),
    CONSTRAINT ck_besoin_qte CHECK (quantite_demandee > 0)
);

-- ---------- Conventions & collaborations ----------
CREATE TABLE partenaire (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nom             VARCHAR(200) NOT NULL,
    type_partenaire VARCHAR(20) NOT NULL,
    contact_email   VARCHAR(180),
    pays            VARCHAR(100),
    description     TEXT,
    CONSTRAINT ck_partenaire_type CHECK (type_partenaire IN ('ACADEMIQUE','INDUSTRIEL','INSTITUTIONNEL'))
);

CREATE TABLE convention (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titre             VARCHAR(250) NOT NULL,
    partenaire_id     BIGINT NOT NULL,
    date_debut        DATE NOT NULL,
    date_fin          DATE,
    statut_convention VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    document_id       BIGINT,
    CONSTRAINT fk_convention_partenaire FOREIGN KEY (partenaire_id) REFERENCES partenaire (id),
    CONSTRAINT fk_convention_document   FOREIGN KEY (document_id)    REFERENCES document (id),
    CONSTRAINT ck_convention_statut CHECK (statut_convention IN ('ACTIVE','EXPIREE','RESILIEE')),
    CONSTRAINT ck_convention_periode CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

-- ---------- Communication interne ----------
CREATE TABLE discussion (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titre              VARCHAR(200) NOT NULL,
    type_discussion    VARCHAR(20) NOT NULL,
    equipe_id          BIGINT,
    evenement_id       BIGINT,
    cree_par_membre_id BIGINT NOT NULL,
    date_creation      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_discussion_equipe    FOREIGN KEY (equipe_id)          REFERENCES equipe (id),
    CONSTRAINT fk_discussion_evenement FOREIGN KEY (evenement_id)       REFERENCES evenement (id),
    CONSTRAINT fk_discussion_createur  FOREIGN KEY (cree_par_membre_id) REFERENCES membre (id),
    CONSTRAINT ck_discussion_type CHECK (type_discussion IN ('GENERALE','EQUIPE','EVENEMENT'))
);

CREATE TABLE message (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    discussion_id    BIGINT NOT NULL,
    auteur_membre_id BIGINT NOT NULL,
    contenu          TEXT NOT NULL,
    date_envoi       TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_message_discussion FOREIGN KEY (discussion_id)    REFERENCES discussion (id),
    CONSTRAINT fk_message_auteur     FOREIGN KEY (auteur_membre_id) REFERENCES membre (id)
);
CREATE INDEX idx_message_discussion ON message (discussion_id);

-- ---------- Notifications ----------
CREATE TABLE notification (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    destinataire_membre_id BIGINT NOT NULL,
    type_notification      VARCHAR(30) NOT NULL,
    titre                  VARCHAR(200) NOT NULL,
    contenu                TEXT,
    lien                   VARCHAR(255),
    lu                     BOOLEAN NOT NULL DEFAULT FALSE,
    date_creation          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_notif_destinataire FOREIGN KEY (destinataire_membre_id) REFERENCES membre (id),
    CONSTRAINT ck_notif_type CHECK (type_notification IN
        ('ADHESION_SOUMISE','ADHESION_DECISION','EVENEMENT_CREE','EVENEMENT_RAPPEL',
         'DOCUMENT_AJOUTE','BESOIN_DECISION','RAPPORT_GENERE'))
);
CREATE INDEX idx_notif_destinataire_non_lu ON notification (destinataire_membre_id) WHERE lu = FALSE;

-- =====================================================================
--  Hibernate Envers — historisation technique (audit) du PROFIL et du COMPTE
--  (entités annotées @Audited : Membre, Utilisateur)
-- =====================================================================
CREATE TABLE revinfo (
    rev      INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    revtstmp BIGINT
);

CREATE TABLE utilisateur_aud (
    id                 BIGINT  NOT NULL,
    rev                INTEGER NOT NULL,
    revtype            SMALLINT,
    email              VARCHAR(180),
    mot_de_passe_hash  VARCHAR(100),
    etat_compte        VARCHAR(20),
    est_admin          BOOLEAN,
    token_activation   VARCHAR(120),
    token_expiration   TIMESTAMP,
    date_creation      TIMESTAMP,
    derniere_connexion TIMESTAMP,
    CONSTRAINT pk_utilisateur_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_utilisateur_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE membre_aud (
    id                    BIGINT  NOT NULL,
    rev                   INTEGER NOT NULL,
    revtype               SMALLINT,
    utilisateur_id        BIGINT,
    nom                   VARCHAR(80),
    prenom                VARCHAR(80),
    photo_chemin          VARCHAR(255),
    biographie            TEXT,
    date_naissance        DATE,
    date_embauche         DATE,
    etablissement_origine VARCHAR(160),
    laboratoire_origine   VARCHAR(160),
    CONSTRAINT pk_membre_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_membre_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE membre_centre_interet_aud (
    rev            INTEGER NOT NULL,
    membre_id      BIGINT  NOT NULL,
    centre_interet VARCHAR(120) NOT NULL,
    revtype        SMALLINT,
    CONSTRAINT pk_membre_centre_aud PRIMARY KEY (rev, membre_id, centre_interet),
    CONSTRAINT fk_membre_centre_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);
