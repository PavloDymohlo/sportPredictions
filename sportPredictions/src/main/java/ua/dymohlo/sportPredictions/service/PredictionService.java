package ua.dymohlo.sportPredictions.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.component.MatchParser;
import ua.dymohlo.sportPredictions.dto.request.PredictionRequest;
import ua.dymohlo.sportPredictions.entity.Prediction;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.PredictionRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PredictionService {

    private final UserRepository userRepository;
    private final MatchParser matchParser;
    private final ObjectMapper objectMapper;
    private final PredictionRepository predictionRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int PREDICTION_TTL_DAYS = 3;

    @Transactional
    public void saveUserPredictions(PredictionRequest predictionDTO, String username) {
        log.info("💾 Saving predictions for user: {} on date: {}", username, predictionDTO.getMatchDate());

        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        long sumNewPredictions = matchParser.countTotalMatches(predictionDTO.getPredictions());
        user.setPredictionCount(user.getPredictionCount() + sumNewPredictions);
        user.setLastPredictions(LocalDateTime.now());
        userRepository.save(user);

        try {
            LocalDate matchDate = LocalDate.parse(predictionDTO.getMatchDate(), DATE_FORMATTER);
            String predictionsJson = objectMapper.writeValueAsString(predictionDTO.getPredictions());

            Prediction prediction = predictionRepository
                    .findByUserAndMatchDate(user, matchDate)
                    .orElse(Prediction.builder().user(user).matchDate(matchDate).build());
            prediction.setPredictionsData(predictionsJson);
            prediction.setCreatedAt(LocalDateTime.now());
            predictionRepository.save(prediction);

            log.info("✅ User {} prediction count increased by {} (total: {})",
                    user.getUserName(), sumNewPredictions, user.getPredictionCount());
        } catch (Exception e) {
            log.error("❌ Failed to save predictions for user: {}", username, e);
            throw new RuntimeException("Failed to save predictions", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<PredictionRequest> getUserPredictions(String userName, String matchDate) {
        log.info("🔍 Getting predictions for user: {} on date: {}", userName, matchDate);

        try {
            User user = userRepository.findByUserName(userName)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            LocalDate date = LocalDate.parse(matchDate, DATE_FORMATTER);

            return predictionRepository.findByUserAndMatchDate(user, date)
                    .map(p -> {
                        try {
                            List<Object> predictions = objectMapper.readValue(
                                    p.getPredictionsData(), new TypeReference<>() {});
                            return PredictionRequest.builder()
                                    .userName(userName)
                                    .matchDate(matchDate)
                                    .predictions(predictions)
                                    .build();
                        } catch (Exception e) {
                            log.error("❌ Failed to deserialize predictions for user: {}", userName, e);
                            return null;
                        }
                    });
        } catch (Exception e) {
            log.error("❌ Failed to get predictions for user: {} on date: {}", userName, matchDate, e);
            return Optional.empty();
        }
    }

    @Transactional
    public void cleanupOldPredictions() {
        LocalDate cutoff = LocalDate.now().minusDays(PREDICTION_TTL_DAYS);
        predictionRepository.deleteByMatchDateBefore(cutoff);
        log.info("🗑️ Deleted predictions older than {}", cutoff);
    }
}
