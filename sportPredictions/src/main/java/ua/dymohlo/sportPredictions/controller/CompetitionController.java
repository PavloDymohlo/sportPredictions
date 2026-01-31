package ua.dymohlo.sportPredictions.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.dymohlo.sportPredictions.component.ApiDataParser;
import ua.dymohlo.sportPredictions.dto.request.PredictionDTO;
import ua.dymohlo.sportPredictions.dto.request.UserCompetitionListRequest;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.service.PredictionService;
import ua.dymohlo.sportPredictions.service.UserCompetitionService;

import java.util.Collections;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v0/competitions")
public class CompetitionController {
    private final ApiDataParser apiDataParser;
    private final UserCompetitionService userCompetitionService;
    private final PredictionService predictionService;

    @GetMapping("/list")
    public List<String> getAllCompetition() {
        return Collections.singletonList(apiDataParser.getCompetitionsDataFromCache());
    }

    @PostMapping("/save-user-competitions")
    public ResponseEntity<String> saveUserCompetitions(@RequestBody UserCompetitionListRequest request) {
        userCompetitionService.updateUserCompetitions(request);
        return ResponseEntity.ok("Competitions list saved successfully");
    }

    @GetMapping("/user-competitions/{username}")
    public ResponseEntity<?> getUserCompetitions(@PathVariable String username) {
        List<Competition> competitions = userCompetitionService.getUserCompetitions(username);
        return ResponseEntity.ok(competitions);
    }

    @GetMapping("/match-status")
    public List<Object> getAllMatchesWithPredictionStatus(@RequestHeader("userName") String userName,
                                                          @RequestParam("date") String date) {
        log.info("getAllMatchesWithPredictionStatus:" + userName + " " + date);
        return Collections.singletonList(predictionService.getAllMatchesWithPredictionStatus(userName, date));
    }

    @GetMapping("/get-predictions")
    public ResponseEntity<PredictionDTO> getUsersPredictions(@RequestHeader("userName") String userName,
                                                             @RequestParam("date") String date) {
        PredictionDTO predictions = predictionService.getUserPredictions(userName, date);
        if (predictions == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(predictions);
    }

    @GetMapping("/event")
    public List<Object> getFutureMatchesFromCache(@RequestHeader("userName") String userName,
                                                  @RequestParam("date") String date) {
        Object result = predictionService.getUserFutureMatches(userName, date);
        return Collections.singletonList(result);
    }

    @PostMapping("/send-user-predictions")
    public String sendUsersPredictions(@RequestBody PredictionDTO request) {
        predictionService.saveUserPredictions(request);
        return "Success";
    }
}
