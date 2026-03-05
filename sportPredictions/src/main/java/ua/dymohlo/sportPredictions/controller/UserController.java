package ua.dymohlo.sportPredictions.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.dymohlo.sportPredictions.dto.request.UpdateLanguageRequest;
import ua.dymohlo.sportPredictions.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/user")
public class UserController {

    private final UserService userService;

    @PutMapping("/language")
    public ResponseEntity<Void> updateLanguage(@Valid @RequestBody UpdateLanguageRequest request,
                                               Authentication auth) {
        userService.updateLanguage(auth.getName(), request.getLanguage());
        return ResponseEntity.ok().build();
    }
}