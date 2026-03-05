package ua.dymohlo.sportPredictions.controller;

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

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v0/competitions")
public class CompetitionController {

    private final CompetitionCacheService competitionCacheService;
    private final UserCompetitionService userCompetitionService;

    @GetMapping("/list")
    public ResponseEntity<List<CompetitionResponse>> getAllCompetitions() {
        return ResponseEntity.ok(competitionCacheService.getCompetitionsList());
    }

    @GetMapping("/user-competitions")
    public ResponseEntity<List<CompetitionResponse>> getUserCompetitions(Authentication auth) {
        return ResponseEntity.ok(userCompetitionService.getUserCompetitions(auth.getName()));
    }

    @PostMapping("/save-user-competitions")
    public ResponseEntity<String> saveUserCompetitions(@RequestBody UserCompetitionListRequest request,
                                                       Authentication auth) {
        request.setUserName(auth.getName());
        userCompetitionService.updateUserCompetitions(request);
        return ResponseEntity.ok("Competitions list saved successfully");
    }
}
