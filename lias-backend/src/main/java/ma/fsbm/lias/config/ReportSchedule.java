package ma.fsbm.lias.config;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import ma.fsbm.lias.domain.enums.EtatCompte;
import ma.fsbm.lias.module.science.ActivityService;
import ma.fsbm.lias.repository.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ReportSchedule {

  private final MembreRepository members;
  private final HistoriqueRoleRepository roles;
  private final HistoriqueStatutRepository statuses;
  private final ActivityService service;

  /** Disabled by default; LIAS_REPORT_CRON='0 0 8 2 1 *' enables annual generation. */
  @Scheduled(cron = "${LIAS_REPORT_CRON:-}", zone = "Africa/Casablanca")
  @org.springframework.transaction.annotation.Transactional
  public void annual() {
    var director = members
      .findAll()
      .stream()
      .filter(
        m ->
          m.getUtilisateur().getEtatCompte() == EtatCompte.ACTIF &&
          roles.estDirecteurActif(m.getId()) &&
          statuses.findStatutCourant(m.getId()).orElse("").equals("PERMANENT")
      )
      .findFirst();
    if (director.isEmpty()) return;
    try {
      SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
          director.get().getUtilisateur().getEmail(),
          null,
          List.of(
            new SimpleGrantedAuthority("ROLE_PERMANENT"),
            new SimpleGrantedAuthority("ROLE_DIRECTEUR")
          )
        )
      );
      service.generate(LocalDate.now().getYear() - 1);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
