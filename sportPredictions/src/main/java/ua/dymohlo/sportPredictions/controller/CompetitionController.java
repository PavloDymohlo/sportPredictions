package ua.dymohlo.sportPredictions.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.dymohlo.sportPredictions.component.ApiDataParser;
import ua.dymohlo.sportPredictions.dto.request.UserCompetitionListRequest;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.service.UserCompetitionService;

import java.util.Collections;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/competitions")
public class CompetitionController {
    private final ApiDataParser apiDataParser;
    private final UserCompetitionService userCompetitionService;

    @GetMapping("/list")
    public List<String> getAllCompetition() {
        return Collections.singletonList(apiDataParser.getCompetitionsDataFromCache());
    }

    @PostMapping("/save-user-competitions")
    public ResponseEntity<String> saveUserCompetitions(@RequestBody UserCompetitionListRequest request) {
        try {
            userCompetitionService.updateUserCompetitions(request);
            log.info("Competitions list saved successfully");
            return ResponseEntity.ok("Competitions list saved successfully");
        } catch (Exception e) {
            log.error("Error saving user competitions list", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/user-competitions/{username}")
    public ResponseEntity<?> getUserCompetitions(@PathVariable String username) {
        try {
            List<Competition> competitions = userCompetitionService.getUserCompetitions(username);
            return ResponseEntity.ok(competitions);
        } catch (Exception e) {
            log.error("Error saving user competitions list", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
