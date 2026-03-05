package ua.dymohlo.sportPredictions.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ua.dymohlo.sportPredictions.entity.MatchData;
import ua.dymohlo.sportPredictions.repository.MatchDataRepository;
import ua.dymohlo.sportPredictions.util.MatchParsingUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class MatchParser {

    private final ObjectMapper objectMapper;
    private final MatchDataRepository matchDataRepository;

    public List<Map<String, Object>> getUserMatches(String date, List<String> subscribedTournaments) {
        LocalDate matchDate = LocalDate.parse(date);
        List<Map<String, Object>> allMatches = loadMatchesFromDb(matchDate);
        if (allMatches == null) return Collections.emptyList();

        Set<String> subscribedKeys = new HashSet<>(subscribedTournaments);
        return allMatches.stream()
                .filter(t -> isSubscribed(t, subscribedKeys))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> loadMatchesFromDb(LocalDate date) {
        String json = matchDataRepository.findByMatchDate(date)
                .map(MatchData::getMatchesJson)
                .orElse(null);

        if (json == null || json.isBlank()) return null;

        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("❌ Failed to deserialize match data for date: {}", date, e);
            return null;
        }
    }

    private boolean isSubscribed(Map<String, Object> tournament, Set<String> subscribedKeys) {
        String country = (String) tournament.get("country");
        String tournamentName = (String) tournament.get("tournament");
        if (country == null || tournamentName == null) return false;
        return subscribedKeys.contains(MatchParsingUtils.competitionKey(country, tournamentName));
    }

//    public List<Map<String, Object>> getUserMatches(String date, List<String> subscribedTournaments) {
//        LocalDate matchDate = LocalDate.parse(date);
//
//        String json = matchDataRepository.findByMatchDate(matchDate)
//                .map(MatchData::getMatchesJson)
//                .orElse(null);
//
//        if (json == null || json.isBlank()) {
//            return Collections.emptyList();
//        }
//
//        List<Map<String, Object>> allMatches;
//        try {
//            allMatches = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Collections.emptyList();
//        }
//
//        List<Map<String, Object>> filteredMatches = new ArrayList<>();
//        for (Map<String, Object> tournament : allMatches) {
//            String country = (String) tournament.get("country");
//            String tournamentName = (String) tournament.get("tournament");
//
//            if (country == null || tournamentName == null) continue;
//
//            String key = MatchParsingUtils.competitionKey(country, tournamentName);
//            if (!subscribedTournaments.contains(key)) continue;
//
//            filteredMatches.add(tournament);
//        }
//
//        return filteredMatches;
//    }

    public long countTotalMatches(List<Object> parsedMatches) {
        int totalMatches = 0;
        for (Object obj : parsedMatches) {
            if (obj instanceof List<?> matchInfo && matchInfo.size() == 2) {
                totalMatches++;
            }
        }
        return totalMatches;
    }
}