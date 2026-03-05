package ua.dymohlo.sportPredictions.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.dymohlo.sportPredictions.dto.request.LoginInRequest;
import ua.dymohlo.sportPredictions.dto.request.RegisterRequest;
import ua.dymohlo.sportPredictions.dto.response.AuthResponse;
import ua.dymohlo.sportPredictions.secutity.JwtUtils;
import ua.dymohlo.sportPredictions.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v0/auth")
public class AuthController {

    private final AuthService authService;

    private static final int TEN_YEARS_SECONDS = 10 * 365 * 24 * 60 * 60;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request,
                                                        HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        setJwtCookie(response, auth.getToken());
        return ResponseEntity.ok(Map.of("userName", auth.getUserName(), "language", auth.getLanguage()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginIn(@RequestBody LoginInRequest request,
                                                       HttpServletResponse response) {
        AuthResponse auth = authService.loginIn(request);
        setJwtCookie(response, auth.getToken());
        return ResponseEntity.ok(Map.of("userName", auth.getUserName(), "language", auth.getLanguage()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearJwtCookie(response);
        return ResponseEntity.ok().build();
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie",
                JwtUtils.JWT_COOKIE_NAME + "=" + token + "; HttpOnly; Path=/; Max-Age=" + TEN_YEARS_SECONDS + "; SameSite=Lax");
    }

    private void clearJwtCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", JwtUtils.JWT_COOKIE_NAME + "=; HttpOnly; Path=/; Max-Age=0; SameSite=Lax");
    }
}
