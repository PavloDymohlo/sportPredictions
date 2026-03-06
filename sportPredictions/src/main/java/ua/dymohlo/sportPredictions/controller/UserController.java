package ua.dymohlo.sportPredictions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.dymohlo.sportPredictions.dto.request.UpdateLanguageRequest;
import ua.dymohlo.sportPredictions.service.UserService;

@Tag(name = "User", description = "User profile settings. Requires authentication.")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/user")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Update interface language",
            description = "Saves the user's preferred language (en or uk). " +
                    "Used to persist the language selection across sessions and for Telegram notifications.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Language updated."),
            @ApiResponse(responseCode = "400", description = "Invalid language code.")
    })
    @PutMapping("/language")
    public ResponseEntity<Void> updateLanguage(@Valid @RequestBody UpdateLanguageRequest request,
                                               Authentication auth) {
        userService.updateLanguage(auth.getName(), request.getLanguage());
        return ResponseEntity.ok().build();
    }
}