package ma.fsbm.lias.module.auth.dto;

public record LoginResponse(
  String accessToken,
  long expiresIn,
  ProfilAuthDto profil
) {}
