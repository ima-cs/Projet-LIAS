package ma.fsbm.lias.module.auth.dto;

import java.util.List;

public record MeResponse(ProfilAuthDto profil, List<String> autorites) {}
