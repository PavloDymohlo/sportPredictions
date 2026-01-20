package ua.dymohlo.sportPredictions.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ua.dymohlo.sportPredictions.component.MatchParser;
import ua.dymohlo.sportPredictions.dto.request.PredictionDTO;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.UserCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PredictionService {
    private final UserRepository userRepository;
    private final MatchParser matchParser;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MatchService matchService;
    private final ObjectMapper objectMapper;
    private final UserCompetitionRepository userCompetitionRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @CachePut(value = "userPredictions", key = "#predictionDTO.userName + '_' + #predictionDTO.matchDate")
    public PredictionDTO saveUserPredictions(PredictionDTO predictionDTO) {
        log.info("💾 Caching predictions for user: {} on date: {}",
                predictionDTO.getUserName(), predictionDTO.getMatchDate());

        Optional<User> optionalUser = userRepository.findByUserName(predictionDTO.getUserName());
        User user = optionalUser.get();
        long sumNewPredictions = matchParser.countTotalMatches(predictionDTO.getPredictions());
        user.setPredictionCount(user.getPredictionCount() + sumNewPredictions);
        user.setLastPredictions(LocalDateTime.now());
        userRepository.save(user);

        log.info("✅ User {} prediction count increased by {} (total: {})",
                user.getUserName(), sumNewPredictions, user.getPredictionCount());

        return predictionDTO;
    }

    public List<Object> getAllMatchesWithPredictionStatus(String userName, String date) {
        String json = matchService.getMatchesFromCacheByDate(date);

        if (json == null || json.isBlank()) {
            log.warn("⚠️ No matches found in cache for date: {}", date);
            return Collections.emptyList();
        }

        List<Object> allMatches;
        try {
            allMatches = objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            log.error("❌ Failed to parse matches JSON", e);
            return Collections.emptyList();
        }

        List<Object> correctPredictions = getCorrectPredictions(userName, date);
        log.info("🎯 Correct predictions count: {}", correctPredictions.size());

        List<Object> result = new ArrayList<>();

        for (Object item : allMatches) {
            if (item instanceof Map) {
                Map<String, Object> tournament = (Map<String, Object>) item;

                if (tournament.containsKey("tournament") && tournament.containsKey("match")) {
                    List<Object> matches = (List<Object>) tournament.get("match");
                    List<Map<String, Object>> processedMatches = new ArrayList<>();

                    for (Object match : matches) {
                        boolean isCorrect = correctPredictions.stream()
                                .anyMatch(pred -> matchesAreEqual(pred, match));

                        log.debug("📋 Match: {} - Correct: {}", match, isCorrect);

                        Map<String, Object> matchWithStatus = new LinkedHashMap<>();
                        matchWithStatus.put("match", match);
                        matchWithStatus.put("predictedCorrectly", isCorrect);

                        processedMatches.add(matchWithStatus);
                    }

                    Map<String, Object> tournamentWithStatus = new LinkedHashMap<>();
                    tournamentWithStatus.put("country", tournament.get("country"));
                    tournamentWithStatus.put("tournament", tournament.get("tournament"));
                    tournamentWithStatus.put("matches", processedMatches);

                    result.add(tournamentWithStatus);
                }
            }
        }

        log.info("✅ Prepared {} tournaments with {} total matches", result.size(),
                result.stream().filter(t -> t instanceof Map)
                        .mapToInt(t -> ((List)((Map)t).get("matches")).size()).sum());
        return result;
    }

    public List<Object> getCorrectPredictions(String userName, String date) {
        log.info("🔍 Getting correct predictions for user: {} on date: {}", userName, date);

        String json = matchService.getMatchesFromCacheByDate(date);

        if (json == null || json.isBlank()) {
            log.warn("⚠️ No match results found for date: {}", date);
            return Collections.emptyList();
        }

        try {
            List<Object> results = objectMapper.readValue(json, List.class);
            log.info("📊 Found {} result items", results.size());

            PredictionDTO predictions = getUserPredictions(userName, date);

            if (predictions == null) {
                log.warn("⚠️ No predictions found for user: {} on date: {}", userName, date);
                return Collections.emptyList();
            }

            if (predictions.getPredictions() == null || predictions.getPredictions().isEmpty()) {
                log.warn("⚠️ Predictions list is empty for user: {}", userName);
                return Collections.emptyList();
            }

            log.info("✅ Found {} predictions for user", predictions.getPredictions().size());

            List<Object> onlyMatchResult = matchesResultParser(results);
            List<Object> userPredictions = userPredictionsParser(predictions);

            log.info("📊 Match results: {}, User predictions: {}",
                    onlyMatchResult.size(), userPredictions.size());

            List<Object> correctResult = new ArrayList<>();
            int size = Math.min(onlyMatchResult.size(), userPredictions.size());

            for (int i = 0; i < size; i++) {
                Object matchResult = onlyMatchResult.get(i);
                Object userPrediction = userPredictions.get(i);

                if (matchesAreEqual(matchResult, userPrediction)) {
                    correctResult.add(matchResult);
                    log.info("✅ Match {} is CORRECT: {}", i, matchResult);
                }
            }

            log.info("🎯 Total correct predictions: {}/{}", correctResult.size(), size);
            return correctResult;

        } catch (Exception e) {
            log.error("❌ Error in getCorrectPredictions", e);
            throw new RuntimeException("Error: " + date, e);
        }
    }

    @Cacheable(value = "userPredictions", key = "#userName + '_' + #matchDate", unless = "#result == null")
    public PredictionDTO getUserPredictions(String userName, String matchDate) {
        log.info("🔍 Getting predictions for user: {} on date: {}", userName, matchDate);

        String key = "userPredictions::" + userName + "_" + matchDate;
        log.info("🔑 Redis key: {}", key);

        Object cachedValue = redisTemplate.opsForValue().get(key);

        if (cachedValue == null) {
            log.warn("❌ No cached predictions found for key: {}", key);
            return null;
        }

        log.info("✅ Found cached value of type: {}", cachedValue.getClass().getName());

        if (cachedValue instanceof PredictionDTO) {
            PredictionDTO result = (PredictionDTO) cachedValue;
            log.info("✅ Returning PredictionDTO with {} predictions",
                    result.getPredictions() != null ? result.getPredictions().size() : 0);
            return result;
        }

        try {
            PredictionDTO result = objectMapper.convertValue(cachedValue, PredictionDTO.class);
            log.info("✅ Converted to PredictionDTO with {} predictions",
                    result.getPredictions() != null ? result.getPredictions().size() : 0);
            return result;
        } catch (Exception e) {
            log.error("❌ Failed to convert cached value", e);
            return null;
        }
    }

    private List<Object> matchesResultParser(List<Object> results) {
        List<Object> onlyMatchResults = new ArrayList<>();

        for (Object result : results) {
            if (result instanceof Map) {
                Map<String, Object> tournament = (Map<String, Object>) result;

                if (tournament.containsKey("match")) {
                    Object matchesObj = tournament.get("match");

                    if (matchesObj instanceof List) {
                        List<Object> matches = (List<Object>) matchesObj;
                        onlyMatchResults.addAll(matches);
                    }
                }
            }
        }

        log.info("📊 Extracted {} match results", onlyMatchResults.size());
        return onlyMatchResults;
    }

    private List<Object> userPredictionsParser(PredictionDTO predictions) {
        List<Object> userPredictions = new ArrayList<>();

        if (predictions == null || predictions.getPredictions() == null) {
            log.warn("⚠️ Predictions is null");
            return userPredictions;
        }

        for (Object prediction : predictions.getPredictions()) {
            if (prediction instanceof List) {
                userPredictions.add(prediction);
            }
        }

        log.info("📊 Extracted {} user predictions", userPredictions.size());
        return userPredictions;
    }

    private boolean matchesAreEqual(Object matchResult, Object userPrediction) {
        if (!(matchResult instanceof List) || !(userPrediction instanceof List)) {
            return false;
        }

        List<?> matchList = (List<?>) matchResult;
        List<?> predictionList = (List<?>) userPrediction;

        if (matchList.size() != 2 || predictionList.size() != 2) {
            return false;
        }

        String team1Result = String.valueOf(matchList.get(0));
        String team1Prediction = String.valueOf(predictionList.get(0));
        String team2Result = String.valueOf(matchList.get(1));
        String team2Prediction = String.valueOf(predictionList.get(1));

        int team1ResultScore = extractScore(team1Result);
        int team1PredictionScore = extractScore(team1Prediction);
        int team2ResultScore = extractScore(team2Result);
        int team2PredictionScore = extractScore(team2Prediction);

        if (team1ResultScore == -1 || team2ResultScore == -1 ||
                team1PredictionScore == -1 || team2PredictionScore == -1) {
            return false;
        }

        return team1ResultScore == team1PredictionScore &&
                team2ResultScore == team2PredictionScore;
    }

    private int extractScore(String teamResult) {
        if (teamResult == null || "н/в".equals(teamResult)) {
            return -1;
        }
        int bracketIndex = teamResult.indexOf("(");
        String mainPart = bracketIndex > 0 ? teamResult.substring(0, bracketIndex).trim() : teamResult.trim();
        String[] parts = mainPart.split(" ");
        if (parts.length == 0) {
            return -1;
        }
        try {
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public List<Object> getUserFutureMatches(String userName, String date) {
        String json = matchService.getMatchesFromCacheByDate(date);
        log.info("📊 Future matches data before filter for {}: {}", userName, json != null ? "found" : "null");

        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }

        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found: " + userName));

        Set<String> allowedCompetitions =
                userCompetitionRepository.findByUser(user).stream()
                        .map(uc -> uc.getCompetition().getCountry() + "|" +
                                uc.getCompetition().getName())
                        .collect(Collectors.toSet());

        try {
            ArrayNode root = (ArrayNode) objectMapper.readTree(json);
            ArrayNode filtered = objectMapper.createArrayNode();

            for (JsonNode node : root) {
                String country = node.path("country").asText();
                String tournament = node.path("tournament").asText();

                if (allowedCompetitions.contains(country + "|" + tournament)) {
                    filtered.add(node);
                }
            }

            List<Object> result = objectMapper.convertValue(filtered, List.class);
            log.info("✅ Filtered matches for user {}: {}", userName, result.size());
            return result;
        } catch (JsonProcessingException e) {
            log.error("❌ Error filtering matches", e);
            throw new RuntimeException(e);
        }
    }

    public void processUserPredictionResults(String userName, String date) {
        log.info("📊 Processing results for user: {} on date: {}", userName, date);

        Optional<User> optionalUser = userRepository.findByUserName(userName);
        if (!optionalUser.isPresent()) {
            log.warn("⚠️ User not found: {}", userName);
            return;
        }

        User user = optionalUser.get();

        List<Object> correctResults = getCorrectPredictions(userName, date);
        int userPoints = correctResults.size();

        log.info("🎯 User {} got {}/{} correct predictions",
                userName, userPoints, user.getPredictionCount());

        long oldScore = user.getTotalScore();
        user.setTotalScore(oldScore + userPoints);
        user.setPercentGuessedMatches(updatePercentGuessedMatches(user));

        userRepository.save(user);

        log.info("💾 Saved user {} - Score: {} -> {} (+{}), Percent: {}%",
                userName, oldScore, user.getTotalScore(), userPoints,
                user.getPercentGuessedMatches());
    }

    public void countAllUsersPredictionsResult() {
        log.info("🎯 ========== STARTING DAILY PREDICTION RESULTS CALCULATION ==========");

        List<User> users = userRepository.findAll();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        String date = yesterday.format(DATE_FORMATTER);

        log.info("📅 Processing predictions for date: {} (ISO format)", date);
        log.info("👥 Found {} users to process", users.size());

        int processedUsers = 0;
        int totalCorrectPredictions = 0;
        int usersWithPredictions = 0;

        for (User user : users) {
            try {
                log.info("👤 Checking user: {}", user.getUserName());

                PredictionDTO predictions = getUserPredictions(user.getUserName(), date);

                if (predictions != null && predictions.getPredictions() != null &&
                        !predictions.getPredictions().isEmpty()) {

                    usersWithPredictions++;
                    log.info("✅ User {} has {} predictions for {}",
                            user.getUserName(), predictions.getPredictions().size(), date);

                    processUserPredictionResults(user.getUserName(), date);
                    processedUsers++;

                    List<Object> correct = getCorrectPredictions(user.getUserName(), date);
                    totalCorrectPredictions += correct.size();

                } else {
                    log.info("⏭️ Skipping user {} - no predictions for {}",
                            user.getUserName(), date);
                }
            } catch (Exception e) {
                log.error("❌ Error processing user {}: {}", user.getUserName(), e.getMessage(), e);
            }
        }

        log.info("🎉 ========== CALCULATION COMPLETE ==========");
        log.info("📊 Users with predictions: {}/{}", usersWithPredictions, users.size());
        log.info("📊 Successfully processed: {}", processedUsers);
        log.info("📊 Total correct predictions: {}", totalCorrectPredictions);
        log.info("📊 Average accuracy: {}%",
                usersWithPredictions > 0 ? (totalCorrectPredictions * 100 / usersWithPredictions) : 0);
    }

    private int updatePercentGuessedMatches(User user) {
        long userPredictionCount = user.getPredictionCount();

        if (userPredictionCount == 0) {
            log.warn("⚠️ User {} has 0 predictions, returning 0%", user.getUserName());
            return 0;
        }

        double percent = ((double) user.getTotalScore() / userPredictionCount) * 100;
        int roundedPercent = (int) Math.round(percent);

        log.debug("📊 User {} stats: {}/{} predictions correct = {}%",
                user.getUserName(), user.getTotalScore(), userPredictionCount, roundedPercent);

        return roundedPercent;
    }
}