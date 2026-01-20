package ua.dymohlo.sportPredictions.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ua.dymohlo.sportPredictions.service.FootballApiService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApiDataParser {
    private final ObjectMapper objectMapper;
    private final FootballApiService footballApiService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String COUNTRIES_CACHE_KEY = "countries";
    private static final String PAST_MATCHES_CACHE_KEY = "past_matches";

    public String parseCompetitionsData() {
        try {
            ResponseEntity<String> response = footballApiService.getCompetitions();
            String responseBody = response.getBody();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode competitionsNode = rootNode.path("competitions");

            ArrayNode resultArray = objectMapper.createArrayNode();

            if (competitionsNode.isArray()) {
                for (JsonNode competition : competitionsNode) {
                    String competitionName = competition.path("name").asText();
                    String countryName = competition.path("area").path("name").asText();
                    String competitionCode = competition.path("code").asText();

                    ObjectNode entry = objectMapper.createObjectNode();
                    entry.put("country", countryName);
                    entry.put("name", competitionName);
                    entry.put("code", competitionCode);
                    resultArray.add(entry);
                }
            }

            String resultJson = objectMapper.writeValueAsString(resultArray);

            redisTemplate.opsForValue().set(COUNTRIES_CACHE_KEY, resultJson);
            log.info("Countries competitions data saved to Redis cache with key: {}", COUNTRIES_CACHE_KEY);

            return resultJson;
        } catch (JsonProcessingException e) {
            log.error("Error parsing competitions data", e);
            return "[]";
        } catch (Exception e) {
            log.error("Error getting competitions data", e);
            return "[]";
        }
    }

    public String getCompetitionsDataFromCache() {
        Object cachedData = redisTemplate.opsForValue().get(COUNTRIES_CACHE_KEY);
        if (cachedData != null) {
            log.info("Retrieved countries competitions data from Redis cache");
            return cachedData.toString();
        }

        log.info("No countries competitions data found in cache, fetching from API");
        return parseCompetitionsData();
    }

    public String parseAndCachePastMatches(){
        LocalDate pastDate = LocalDate.now().minusDays(1);
        return parseAndCacheMatchesFromApi(pastDate);
    }
    public String parseAndCacheFutureMatches(){
        LocalDate futureDate = LocalDate.now().plusDays(3);
        return parseAndCacheMatchesFromApi(futureDate);
    }

    public String parseAndCacheMatchesFromApi(LocalDate date) {
        try {
            String cacheKey = "matches_" + date.format(DateTimeFormatter.ofPattern("dd_MM_yyyy"));

            ResponseEntity<List<String>> response = footballApiService.getMatchesFromApi(date);
            List<String> allMatchesResponses = response.getBody();

            if (allMatchesResponses == null || allMatchesResponses.isEmpty()) {
                log.warn("No past matches fetched for date: {}", date);
                return "[]";
            }

            ArrayNode resultArray = objectMapper.createArrayNode();

            for (String json : allMatchesResponses) {
                JsonNode rootNode = objectMapper.readTree(json);
                JsonNode matchesNode = rootNode.path("matches");

                if (!matchesNode.isArray()) continue;

                String country = "";
                if (matchesNode.size() > 0) {
                    country = matchesNode.get(0).path("area").path("name").asText("");
                }

                String tournament = rootNode.path("competition").path("name").asText("");

                ObjectNode competitionNode = objectMapper.createObjectNode();
                competitionNode.put("country", country);
                competitionNode.put("tournament", tournament);

                ArrayNode matchArray = objectMapper.createArrayNode();

                for (JsonNode match : matchesNode) {
                    ArrayNode singleMatch = objectMapper.createArrayNode();

                    String home = match.path("homeTeam").path("name").asText();
                    String away = match.path("awayTeam").path("name").asText();

                    String homeScore = match.path("score").path("fullTime").path("home").asText();
                    String awayScore = match.path("score").path("fullTime").path("away").asText();

                    singleMatch.add(home + " " + homeScore);
                    singleMatch.add(away + " " + awayScore);

                    matchArray.add(singleMatch);
                }

                competitionNode.set("match", matchArray);
                resultArray.add(competitionNode);
            }


            String resultJson = objectMapper.writeValueAsString(resultArray);
            redisTemplate.opsForValue().set(cacheKey, resultJson);

            log.info("✅ Past matches data saved to Redis cache with key: {}", cacheKey);
            log.info("📊 Cached {} tournaments with matches", resultArray.size());

            return resultJson;

        } catch (Exception e) {
            log.error("❌ Error parsing and caching past matches", e);
            return "[]";
        }
    }
//
//    public String getPastMatchesFromCache() {
//        Object cachedData = redisTemplate.opsForValue().get(PAST_MATCHES_CACHE_KEY);
//        if (cachedData != null) {
//            log.info("Retrieved past matches from Redis cache");
//            return cachedData.toString();
//        }
//        log.info("No past matches in cache, fetching from API");
//        return parseAndCachePastMatches();
//    }
}