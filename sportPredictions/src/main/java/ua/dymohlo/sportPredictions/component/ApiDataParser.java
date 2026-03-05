package ua.dymohlo.sportPredictions.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApiDataParser {
    private final ObjectMapper objectMapper;

    public String parseCompetitions(String responseBody) {
        try {
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

            return objectMapper.writeValueAsString(resultArray);
        } catch (JsonProcessingException e) {
            log.error("Error parsing competitions data", e);
            return "[]";
        }
    }

    public String parseMatches(List<String> allMatchesResponses, LocalDate date) {
        try {
            if (allMatchesResponses == null || allMatchesResponses.isEmpty()) {
                log.warn("No matches fetched for date: {}", date);
                return "[]";
            }

            ArrayNode resultArray = objectMapper.createArrayNode();

            for (String json : allMatchesResponses) {
                JsonNode rootNode = objectMapper.readTree(json);
                JsonNode matchesNode = rootNode.path("matches");
                if (!matchesNode.isArray()) continue;
                resultArray.add(buildCompetitionNode(rootNode, matchesNode));
            }

            String resultJson = objectMapper.writeValueAsString(resultArray);
            log.info("✅ Parsed {} tournaments with matches for date: {}", resultArray.size(), date);
            return resultJson;

        } catch (Exception e) {
            log.error("❌ Error parsing matches", e);
            return "[]";
        }
    }

    private ObjectNode buildCompetitionNode(JsonNode rootNode, JsonNode matchesNode) {
        ObjectNode competitionNode = objectMapper.createObjectNode();
        competitionNode.put("country", extractCountry(rootNode, matchesNode));
        competitionNode.put("tournament", rootNode.path("competition").path("name").asText(""));
        competitionNode.set("match", buildMatchArray(matchesNode));
        return competitionNode;
    }

    private String extractCountry(JsonNode rootNode, JsonNode matchesNode) {
        String country = rootNode.path("competition").path("area").path("name").asText("");
        if (country.isEmpty() && !matchesNode.isEmpty()) {
            country = matchesNode.get(0).path("area").path("name").asText("");
        }
        return country;
    }

    private ArrayNode buildMatchArray(JsonNode matchesNode) {
        ArrayNode matchArray = objectMapper.createArrayNode();
        for (JsonNode match : matchesNode) {
            ArrayNode singleMatch = objectMapper.createArrayNode();
            singleMatch.add(match.path("homeTeam").path("name").asText() + " " +
                    match.path("score").path("fullTime").path("home").asText());
            singleMatch.add(match.path("awayTeam").path("name").asText() + " " +
                    match.path("score").path("fullTime").path("away").asText());
            matchArray.add(singleMatch);
        }
        return matchArray;
    }

//    public String parseMatches(List<String> allMatchesResponses, LocalDate date) {
//        try {
//            if (allMatchesResponses == null || allMatchesResponses.isEmpty()) {
//                log.warn("No matches fetched for date: {}", date);
//                return "[]";
//            }
//
//            ArrayNode resultArray = objectMapper.createArrayNode();
//
//            for (String json : allMatchesResponses) {
//                JsonNode rootNode = objectMapper.readTree(json);
//                JsonNode matchesNode = rootNode.path("matches");
//                if (!matchesNode.isArray()) continue;
//
//                String country = rootNode.path("competition").path("area").path("name").asText("");
//                if (country.isEmpty() && !matchesNode.isEmpty()) {
//                    country = matchesNode.get(0).path("area").path("name").asText("");
//                }
//
//                String tournament = rootNode.path("competition").path("name").asText("");
//
//                ObjectNode competitionNode = objectMapper.createObjectNode();
//                competitionNode.put("country", country);
//                competitionNode.put("tournament", tournament);
//
//                ArrayNode matchArray = objectMapper.createArrayNode();
//                for (JsonNode match : matchesNode) {
//                    ArrayNode singleMatch = objectMapper.createArrayNode();
//                    singleMatch.add(match.path("homeTeam").path("name").asText() + " " +
//                            match.path("score").path("fullTime").path("home").asText());
//                    singleMatch.add(match.path("awayTeam").path("name").asText() + " " +
//                            match.path("score").path("fullTime").path("away").asText());
//                    matchArray.add(singleMatch);
//                }
//
//                competitionNode.set("match", matchArray);
//                resultArray.add(competitionNode);
//            }
//
//            String resultJson = objectMapper.writeValueAsString(resultArray);
//            log.info("✅ Parsed {} tournaments with matches for date: {}", resultArray.size(), date);
//            return resultJson;
//
//        } catch (Exception e) {
//            log.error("❌ Error parsing matches", e);
//            return "[]";
//        }
//    }
}