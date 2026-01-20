package ua.dymohlo.sportPredictions.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MatchParser {

    private final ObjectMapper objectMapper;

//    public List<Object> parseMatches(String json) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        List<Object> result = new ArrayList<>();
//        try {
//            List<Map<String, Object>> matches = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
//            });
//            for (Map<String, Object> match : matches) {
//                if (match.containsKey("date") && result.isEmpty()) {
//                    result.add(Map.of("date", match.get("date")));
//                }
//                if (match.containsKey("country") && match.containsKey("tournament")) {
//                    result.add(Map.of("country", match.get("country"), "tournament", match.get("tournament")));
//                }
//                if (match.containsKey("matches")) {
//                    List<List<String>> matchDetails = (List<List<String>>) match.get("matches");
//                    for (List<String> game : matchDetails) {
//                        result.add(game);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return result;
//    }

    public long countTotalMatches(List<Object> parsedMatches) {
        int totalMatches = 0;
        for (Object obj : parsedMatches) {
            if (obj instanceof List) {
                List<?> matchInfo = (List<?>) obj;
                if (matchInfo.size() == 2) {
                    totalMatches++;
                }
            }
        }
        return totalMatches;
    }
}