package ma.fsbm.lias.config;

import jakarta.servlet.http.HttpServletResponse;
import ma.fsbm.lias.config.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
    HttpSecurity http,
    JwtAuthenticationFilter jwtAuthenticationFilter
  ) throws Exception {
    return http
      .csrf(csrf -> csrf.disable())
      .cors(cors ->
        cors.configurationSource(request -> {
          var c = new org.springframework.web.cors.CorsConfiguration();
          c.setAllowedOrigins(
            java.util.List.of("http://localhost:5173", "http://127.0.0.1:5173")
          );
          c.setAllowedMethods(
            java.util.List.of("GET", "POST", "PUT", "PATCH", "OPTIONS")
          );
          c.setAllowedHeaders(
            java.util.List.of("Authorization", "Content-Type")
          );
          return c;
        })
      )

      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )

      .authorizeHttpRequests(auth ->
        auth
          .requestMatchers(
            "/actuator/health",
            "/api/public/**",
            "/api/auth/login",
            "/api/auth/activer"
          )
          .permitAll()
          .anyRequest()
          .authenticated()
      )

      .exceptionHandling(exception ->
        exception
          .authenticationEntryPoint((request, response, authException) ->
            writeSecurityError(
              request,
              response,
              401,
              "Authentification requise."
            )
          )
          .accessDeniedHandler((request, response, error) ->
            writeSecurityError(request, response, 403, "Accès interdit.")
          )
      )

      .formLogin(form -> form.disable())
      .httpBasic(basic -> basic.disable())

      .addFilterBefore(
        jwtAuthenticationFilter,
        UsernamePasswordAuthenticationFilter.class
      )

      .build();
  }

  private static void writeSecurityError(
    jakarta.servlet.http.HttpServletRequest request,
    HttpServletResponse response,
    int status,
    String message
  ) throws java.io.IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    var body = new ma.fsbm.lias.common.error.ApiError(
      java.time.Instant.now(),
      status,
      org.springframework.http.HttpStatus.valueOf(status).getReasonPhrase(),
      message,
      request.getRequestURI()
    );
    new com.fasterxml.jackson.databind.ObjectMapper()
      .findAndRegisterModules()
      .writeValue(response.getOutputStream(), body);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
