package ma.fsbm.lias.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final ma.fsbm.lias.module.auth.AuthService authService;

  public JwtAuthenticationFilter(
    JwtService jwtService,
    ma.fsbm.lias.module.auth.AuthService authService
  ) {
    this.authService = authService;
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    String authorization = request.getHeader("Authorization");

    if (authorization == null || !authorization.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authorization.substring(7);

    if (
      jwtService.isTokenValid(token) &&
      SecurityContextHolder.getContext().getAuthentication() == null
    ) {
      String email = jwtService.extractEmail(token);

      java.util.List<String> current;
      try {
        current = authService.me(email).autorites();
      } catch (org.springframework.web.server.ResponseStatusException ex) {
        filterChain.doFilter(request, response);
        return;
      }
      var authorities = current
        .stream()
        .map(SimpleGrantedAuthority::new)
        .toList();

      var authentication = new UsernamePasswordAuthenticationToken(
        email,
        null,
        authorities
      );

      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }
}
