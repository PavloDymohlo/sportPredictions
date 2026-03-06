package ua.dymohlo.sportPredictions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.dymohlo.sportPredictions.dto.request.UserCompetitionListRequest;
import ua.dymohlo.sportPredictions.dto.response.CompetitionResponse;
import ua.dymohlo.sportPredictions.service.CompetitionCacheService;
import ua.dymohlo.sportPredictions.service.UserCompetitionService;

import java.util.List;

@Tag(name = "Competitions", description = "Available competitions and user competition preferences. Requires authentication.")
@SecurityRequirement(name = "cookieAuth")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v0/competitions")
public class CompetitionController {

    private final CompetitionCacheService competitionCacheService;
    private final UserCompetitionService userCompetitionService;

    @Operation(summary = "Get all available competitions",
            description = "Returns all competitions available in the system (fetched from the football API).")
    @ApiResponse(responseCode = "200", description = "List of competitions.")
    @GetMapping("/list")
    public ResponseEntity<List<CompetitionResponse>> getAllCompetitions() {
        return ResponseEntity.ok(competitionCacheService.getCompetitionsList());
    }

    @Operation(summary = "Get current user's competitions",
            description = "Returns competitions the authenticated user has subscribed to for predictions.")
    @ApiResponse(responseCode = "200", description = "List of user's competitions.")
    @GetMapping("/user-competitions")
    public ResponseEntity<List<CompetitionResponse>> getUserCompetitions(Authentication auth) {
        return ResponseEntity.ok(userCompetitionService.getUserCompetitions(auth.getName()));
    }

    @Operation(summary = "Save user's competition preferences",
            description = "Replaces the user's competition list. Changes take effect from the following day after the nightly update.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saved successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid request body.")
    })
    @PostMapping("/save-user-competitions")
    public ResponseEntity<String> saveUserCompetitions(@RequestBody UserCompetitionListRequest request,
                                                       Authentication auth) {
        request.setUserName(auth.getName());
        userCompetitionService.updateUserCompetitions(request);
        return ResponseEntity.ok("Competitions list saved successfully");
    }
}
