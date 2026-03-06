package ua.dymohlo.sportPredictions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.dymohlo.sportPredictions.dto.request.PredictionRequest;
import ua.dymohlo.sportPredictions.dto.response.TournamentWithStatusResponse;
import ua.dymohlo.sportPredictions.service.PredictionService;
import ua.dymohlo.sportPredictions.service.UserMatchesService;

import java.util.List;
import java.util.Map;

@Tag(name = "Predictions", description = "Match listings and score predictions. Requires authentication.")
@SecurityRequirement(name = "cookieAuth")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v0/predictions")
public class PredictionController {

    private final PredictionService predictionService;
    private final UserMatchesService userMatchesService;

    @Operation(summary = "Get matches with prediction status",
            description = "Returns all matches for the given date grouped by competition, " +
                    "each with the user's prediction and whether it was correct. " +
                    "Only includes competitions the user is subscribed to.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of competitions with match statuses."),
            @ApiResponse(responseCode = "401", description = "Not authenticated.")
    })
    @GetMapping("/match-status")
    public ResponseEntity<List<TournamentWithStatusResponse>> getAllMatchesWithPredictionStatus(
            @Parameter(description = "Date in ISO format (yyyy-MM-dd)", example = "2025-03-10")
            @RequestParam("date") String date, Authentication auth) {
        log.info("getAllMatchesWithPredictionStatus: {} {}", auth.getName(), date);
        return ResponseEntity.ok(userMatchesService.getAllMatchesWithPredictionStatus(auth.getName(), date));
    }

    @Operation(summary = "Get user's saved predictions for a date",
            description = "Returns the prediction object the user saved for a specific date. Returns 204 if none exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Predictions found."),
            @ApiResponse(responseCode = "204", description = "No predictions saved for this date.")
    })
    @GetMapping
    public ResponseEntity<PredictionRequest> getUserPredictions(
            @Parameter(description = "Date in ISO format (yyyy-MM-dd)", example = "2025-03-10")
            @RequestParam("date") String date,
            Authentication auth) {
        return predictionService.getUserPredictions(auth.getName(), date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Get future matches for a date",
            description = "Returns upcoming matches for a given date filtered by the user's competition subscriptions. " +
                    "Available up to 3 days in advance. Prediction window closes at midnight on the match day.")
    @ApiResponse(responseCode = "200", description = "List of upcoming matches.")
    @GetMapping("/future-matches")
    public ResponseEntity<List<Map<String, Object>>> getFutureMatches(
            @Parameter(description = "Date in ISO format (yyyy-MM-dd)", example = "2025-03-10")
            @RequestParam("date") String date,
            Authentication auth) {
        return ResponseEntity.ok(userMatchesService.getUserFutureMatches(auth.getName(), date));
    }

    @Operation(summary = "Save predictions for a date",
            description = "Saves or replaces the user's score predictions for all matches on a given date. " +
                    "Only allowed before midnight on the match day.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Predictions saved successfully."),
            @ApiResponse(responseCode = "400", description = "Prediction window has closed for this date.")
    })
    @PostMapping
    public ResponseEntity<String> sendUserPredictions(@RequestBody PredictionRequest request,
                                                      Authentication auth) {
        predictionService.saveUserPredictions(request, auth.getName());
        return ResponseEntity.ok("Success");
    }
}
