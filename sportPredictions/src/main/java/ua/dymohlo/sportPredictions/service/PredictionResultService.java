package ua.dymohlo.sportPredictions.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.dymohlo.sportPredictions.component.MatchParser;
import ua.dymohlo.sportPredictions.dto.request.PredictionRequest;
import ua.dymohlo.sportPredictions.entity.Prediction;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.MatchDataRepository;
import ua.dymohlo.sportPredictions.repository.PredictionRepository;
import ua.dymohlo.sportPredictions.repository.UserCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;
import ua.dymohlo.sportPredictions.util.MatchParsingUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PredictionResultService {

    private final UserRepository userRepository;
    private final UserCompetitionRepository userCompetitionRepository;
    private final MatchParser matchParser;
    private final PredictionRepository predictionRepository;
    private final MatchDataRepository matchDataRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void countAllUsersPredictionsResult() {
        log.info("========== STARTING DAILY PREDICTION RESULTS CALCULATION ==========");

        LocalDate yesterday = LocalDate.now().minusDays(1);

        if (!matchDataRepository.existsByMatchDate(yesterday)) {
            log.warn("No match data for {} — skipping prediction scoring to avoid data corruption", yesterday);
            return;
        }

        List<Prediction> predictions = predictionRepository.findByMatchDateWithUser(yesterday);
        String date = yesterday.format(DATE_FORMATTER);
        log.info("Processing predictions for date: {} ({} users)", date, predictions.size());

        int processedUsers = 0;
        int totalCorrectPredictions = 0;

        for (Prediction prediction : predictions) {
            try {
                int correctCount = processUserPredictionResults(prediction, date);
                processedUsers++;
                totalCorrectPredictions += correctCount;
            } catch (Exception e) {
                log.error("Error processing user {}", prediction.getUser().getUserName(), e);
            }
        }

        log.info("========== PREDICTION RESULTS CALCULATION COMPLETE ==========");
        log.info("Users with predictions: {}, successfully processed: {}, total correct: {}",
                predictions.size(), processedUsers, totalCorrectPredictions);
    }

    private int processUserPredictionResults(Prediction prediction, String date) {
        User user = prediction.getUser();
        log.debug("Processing results for user: {} on date: {}", user.getUserName(), date);

        List<Object> correctResults = getCorrectPredictions(user, prediction, date);
        int userPoints = correctResults.size();

        long oldScore = user.getTotalScore();
        user.setTotalScore(oldScore + userPoints);
        user.setPercentGuessedMatches(
                MatchParsingUtils.calculateAccuracyPercent(user.getTotalScore(), user.getPredictionCount()));
        userRepository.save(user);

        log.debug("Saved user {} — score: {} -> {} (+{}), accuracy: {}%",
                user.getUserName(), oldScore, user.getTotalScore(), userPoints, user.getPercentGuessedMatches());

        return userPoints;
    }

    private List<Object> getCorrectPredictions(User user, Prediction prediction, String date) {
        log.debug("Getting correct predictions for user: {} on date: {}", user.getUserName(), date);

        List<String> subscribedTournaments = userCompetitionRepository.findByUser(user).stream()
                .map(uc -> MatchParsingUtils.competitionKey(
                        uc.getCompetition().getCountry(), uc.getCompetition().getName()))
                .toList();

        if (subscribedTournaments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> userTournaments = matchParser.getUserMatches(date, subscribedTournaments);

        if (userTournaments.isEmpty()) {
            log.warn("No match results found for date: {}", date);
            return Collections.emptyList();
        }

        List<Object> predictionsList;
        try {
            predictionsList = objectMapper.readValue(prediction.getPredictionsData(), new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize predictions for user: {}", user.getUserName(), e);
            return Collections.emptyList();
        }

        PredictionRequest predictionRequest = PredictionRequest.builder()
                .userName(user.getUserName())
                .matchDate(date)
                .predictions(predictionsList)
                .build();

        List<Object> onlyMatchResult = MatchParsingUtils.extractMatchesFromTournaments(userTournaments);
        List<Object> userPredictions = MatchParsingUtils.extractUserPredictions(predictionRequest);

        List<Object> correctResult = new ArrayList<>();
        for (Object matchResult : onlyMatchResult) {
            for (Object userPrediction : userPredictions) {
                if (MatchParsingUtils.matchesAreEqual(matchResult, userPrediction)) {
                    correctResult.add(matchResult);
                    break;
                }
            }
        }

        log.debug("Correct predictions for user {}: {}/{}", user.getUserName(), correctResult.size(), onlyMatchResult.size());
        return correctResult;
    }
}
