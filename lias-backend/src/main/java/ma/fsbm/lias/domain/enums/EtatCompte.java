package ma.fsbm.lias.domain.enums;

/** État du compte utilisateur (axe indépendant du statut et du rôle). */
public enum EtatCompte {
    ACTIF,       // connexion autorisée
    GELE,        // retraité : connexion bloquée, données conservées
    DESACTIVE    // ancien membre : compte désactivé (réactivable), historique conservé
}
