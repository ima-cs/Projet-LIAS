package ma.fsbm.lias.domain.enums;

/** Statut d'un membre (axe « appartenance »). Historisé dans historique_statut. */
public enum TypeStatut {
    PERMANENT,   // membre officiel du LIAS (interne OU externe à la faculté)
    ASSOCIE,     // collaborateur rattaché à un autre laboratoire
    DOCTORANT,
    RETRAITE,
    ANCIEN
}
