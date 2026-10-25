package ma.fsbm.lias.module.auth.dto;

public record ProfilAuthDto(
  Long membreId,
  String email,
  String nom,
  String prenom
) {}
