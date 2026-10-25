-- Enforce uniqueness of private, single-use activation tokens without changing
-- the business model or the immutable V1 foundation.
CREATE UNIQUE INDEX uidx_utilisateur_token_activation
    ON utilisateur (token_activation) WHERE token_activation IS NOT NULL;
