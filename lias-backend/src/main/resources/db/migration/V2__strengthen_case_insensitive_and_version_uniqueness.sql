-- These constraints enforce the existing service rules during concurrent requests.
-- V1 remains unchanged. An e-mail identifies one account, regardless of case;
-- one document version can have only one immediate successor.
CREATE UNIQUE INDEX uidx_utilisateur_email_normalise ON utilisateur (lower(email));
CREATE UNIQUE INDEX uidx_adhesion_email_en_attente ON demande_adhesion (lower(email))
    WHERE statut_demande = 'EN_ATTENTE';
CREATE UNIQUE INDEX uidx_document_version_suivante ON document (document_parent_id)
    WHERE document_parent_id IS NOT NULL;
