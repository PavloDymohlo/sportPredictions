package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.dymohlo.sportPredictions.component.MatchParser;
import ua.dymohlo.sportPredictions.dto.request.PredictionRequest;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.MatchDataRepository;
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
    private final PredictionService predictionService;
    private final MatchDataRepository matchDataRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void countAllUsersPredictionsResult() {
        log.info("🎯 ========== STARTING DAILY PREDICTION RESULTS CALCULATION ==========");

        LocalDate yesterday = LocalDate.now().minusDays(1);

        if (!matchDataRepository.existsByMatchDate(yesterday)) {
            log.warn("⚠️ No match data for {} — skipping prediction scoring to avoid data corruption", yesterday);
            return;
        }

        List<User> users = userRepository.findAll();
        String date = yesterday.format(DATE_FORMATTER);
        log.info("📅 Processing predictions for date: {}", date);

        int processedUsers = 0;
        int totalCorrectPredictions = 0;
        int usersWithPredictions = 0;

        for (User user : users) {
            try {
                PredictionRequest predictions = predictionService.getUserPredictions(user.getUserName(), date).orElse(null);

                if (predictions != null && predictions.getPredictions() != null
                        && !predictions.getPredictions().isEmpty()) {
                    usersWithPredictions++;
                    int correctCount = processUserPredictionResults(user.getUserName(), date);
                    processedUsers++;
                    totalCorrectPredictions += correctCount;
                } else {
                    log.info("⏭️ Skipping user {} - no predictions for {}", user.getUserName(), date);
                }
            } catch (Exception e) {
                log.error("❌ Error processing user {}: {}", user.getUserName(), e.getMessage(), e);
            }
        }

        log.info("🎉 ========== CALCULATION COMPLETE ==========");
        log.info("📊 Users with predictions: {}/{}", usersWithPredictions, users.size());
        log.info("📊 Successfully processed: {}", processedUsers);
        log.info("📊 Total correct predictions: {}", totalCorrectPredictions);
    }

    private int processUserPredictionResults(String userName, String date) {
        log.info("📊 Processing results for user: {} on date: {}", userName, date);

        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found: " + userName));

        List<Object> correctResults = getCorrectPredictions(userName, date);
        int userPoints = correctResults.size();

        long oldScore = user.getTotalScore();
        user.setTotalScore(oldScore + userPoints);
        user.setPercentGuessedMatches(
                MatchParsingUtils.calculateAccuracyPercent(user.getTotalScore(), user.getPredictionCount()));
        userRepository.save(user);

        log.info("💾 Saved user {} - Score: {} -> {} (+{}), Percent: {}%",
                userName, oldScore, user.getTotalScore(), userPoints, user.getPercentGuessedMatches());

        return userPoints;
    }

    private List<Object> getCorrectPredictions(String userName, String date) {
        log.info("🔍 Getting correct predictions for user: {} on date: {}", userName, date);

        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> subscribedTournaments = userCompetitionRepository.findByUser(user).stream()
                .map(uc -> MatchParsingUtils.competitionKey(
                        uc.getCompetition().getCountry(), uc.getCompetition().getName()))
                .toList();

        if (subscribedTournaments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> userTournaments = matchParser.getUserMatches(date, subscribedTournaments);

        if (userTournaments.isEmpty()) {
            log.warn("⚠️ No match results found for date: {}", date);
            return Collections.emptyList();
        }

        PredictionRequest predictions = predictionService.getUserPredictions(userName, date).orElse(null);

        if (predictions == null || predictions.getPredictions() == null || predictions.getPredictions().isEmpty()) {
            log.info("⏭️ No predictions for user: {} on date: {}", userName, date);
            return Collections.emptyList();
        }

        List<Object> onlyMatchResult = MatchParsingUtils.extractMatchesFromTournaments(userTournaments);
        List<Object> userPredictions = MatchParsingUtils.extractUserPredictions(predictions);

        List<Object> correctResult = new ArrayList<>();
        for (Object matchResult : onlyMatchResult) {
            for (Object userPrediction : userPredictions) {
                if (MatchParsingUtils.matchesAreEqual(matchResult, userPrediction)) {
                    correctResult.add(matchResult);
                    break;
                }
            }
        }

        log.info("🎯 Total correct predictions: {}/{}", correctResult.size(), onlyMatchResult.size());
        return correctResult;
    }
}
