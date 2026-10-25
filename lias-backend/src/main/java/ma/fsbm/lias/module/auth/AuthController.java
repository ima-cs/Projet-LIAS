package ma.fsbm.lias.module.auth;

import ma.fsbm.lias.module.auth.dto.LoginRequest;
import ma.fsbm.lias.module.auth.dto.LoginResponse;
import ma.fsbm.lias.module.auth.dto.MeResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public LoginResponse login(@RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @GetMapping("/me")
  public MeResponse me(Authentication authentication) {
    return authService.me(authentication.getName());
  }
}
