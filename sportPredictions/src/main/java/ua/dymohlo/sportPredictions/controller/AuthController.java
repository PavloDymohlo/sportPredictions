package ua.dymohlo.sportPredictions.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.dymohlo.sportPredictions.dto.request.LoginInRequest;
import ua.dymohlo.sportPredictions.dto.request.RegisterRequest;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.service.AuthService;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest registerRequest, HttpServletResponse response) {
        User user = authService.register(registerRequest);
        response.setHeader("Location", "/office-page");
        response.setStatus(HttpStatus.FOUND.value());
        return user;
    }

    @PostMapping("/login")
    public String loginIn(@RequestBody LoginInRequest loginInRequest, HttpServletResponse response) {
        authService.loginIn(loginInRequest);
        response.setHeader("Location", "/office-page");
        response.setStatus(HttpStatus.FOUND.value());
        return "Success";
    }
}