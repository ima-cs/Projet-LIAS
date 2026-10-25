package ma.fsbm.lias.module.transversal;

import ma.fsbm.lias.domain.enums.TypeNotification;

public record BusinessEvent(
  TypeNotification type,
  String titre,
  String contenu,
  String lien,
  Long destinataire,
  boolean direction
) {}
