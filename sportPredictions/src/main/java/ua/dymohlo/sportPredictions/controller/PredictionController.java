package ua.dymohlo.sportPredictions.controller;

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

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v0/predictions")
public class PredictionController {

    private final PredictionService predictionService;
    private final UserMatchesService userMatchesService;

    @GetMapping("/match-status")
    public ResponseEntity<List<TournamentWithStatusResponse>> getAllMatchesWithPredictionStatus(
            @RequestParam("date") String date, Authentication auth) {
        log.info("getAllMatchesWithPredictionStatus: {} {}", auth.getName(), date);
        return ResponseEntity.ok(userMatchesService.getAllMatchesWithPredictionStatus(auth.getName(), date));
    }

    @GetMapping
    public ResponseEntity<PredictionRequest> getUserPredictions(@RequestParam("date") String date,
                                                                Authentication auth) {
        return predictionService.getUserPredictions(auth.getName(), date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/future-matches")
    public ResponseEntity<List<Map<String, Object>>> getFutureMatches(@RequestParam("date") String date,
                                                                      Authentication auth) {
        return ResponseEntity.ok(userMatchesService.getUserFutureMatches(auth.getName(), date));
    }

    @PostMapping
    public ResponseEntity<String> sendUserPredictions(@RequestBody PredictionRequest request,
                                                      Authentication auth) {
        predictionService.saveUserPredictions(request, auth.getName());
        return ResponseEntity.ok("Success");
    }
}
