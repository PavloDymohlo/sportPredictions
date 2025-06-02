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

@Component
@Slf4j
@RequiredArgsConstructor
public class ApiDataParser {
    private final ObjectMapper objectMapper;
    private final FootballApiService footballApiService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String COUNTRIES_CACHE_KEY = "countries";

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

                    ObjectNode entry = objectMapper.createObjectNode();
                    entry.put(countryName, competitionName);
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
}