package ua.dymohlo.sportPredictions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Authentication", description = "Registration, login and logout. No authentication required.")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v0/auth")
public class AuthController {

    private final AuthService authService;

    private static final int TEN_YEARS_SECONDS = 10 * 365 * 24 * 60 * 60;

    @Operation(summary = "Register a new user",
            description = "Creates a new account. Sets a JWT HttpOnly cookie on success.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registered successfully. Returns userName and language."),
            @ApiResponse(responseCode = "409", description = "Username already exists.")
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request,
                                                        HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        setJwtCookie(response, auth.getToken());
        return ResponseEntity.ok(Map.of("userName", auth.getUserName(), "language", auth.getLanguage()));
    }

    @Operation(summary = "Login",
            description = "Authenticates a user. Sets a JWT HttpOnly cookie valid for 10 years. " +
                    "After calling this endpoint in Swagger UI, all protected endpoints become accessible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful. Returns userName and language."),
            @ApiResponse(responseCode = "401", description = "Invalid credentials.")
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginIn(@RequestBody LoginInRequest request,
                                                       HttpServletResponse response) {
        AuthResponse auth = authService.loginIn(request);
        setJwtCookie(response, auth.getToken());
        return ResponseEntity.ok(Map.of("userName", auth.getUserName(), "language", auth.getLanguage()));
    }

    @Operation(summary = "Logout", description = "Clears the JWT cookie.")
    @ApiResponse(responseCode = "200", description = "Logged out successfully.")
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
