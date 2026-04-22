package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.component.MatchParser;
import ua.dymohlo.sportPredictions.dto.request.PredictionRequest;
import ua.dymohlo.sportPredictions.dto.response.MatchStatusResponse;
import ua.dymohlo.sportPredictions.dto.response.TournamentWithStatusResponse;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.GroupCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.GroupTournamentRepository;
import ua.dymohlo.sportPredictions.repository.UserCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.UserGroupRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;
import ua.dymohlo.sportPredictions.util.MatchParsingUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserMatchesService {

    private final UserRepository userRepository;
    private final UserCompetitionRepository userCompetitionRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupCompetitionRepository groupCompetitionRepository;
    private final GroupTournamentRepository groupTournamentRepository;
    private final MatchParser matchParser;
    private final PredictionService predictionService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserFutureMatches(String userName, String date) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);
        Set<String> competitionKeys = new HashSet<>(getUserTournamentsByDate(user, targetDate));

        PredictionRequest predictionDTO = predictionService.getUserPredictions(userName, date).orElse(null);
        List<Object> userPredictionsList = MatchParsingUtils.extractUserPredictions(predictionDTO);

        if (predictionDTO != null) {
            competitionKeys.addAll(extractCompetitionKeysFromPredictions(predictionDTO));
        }

        if (!userPredictionsList.isEmpty()) {
            return filterMatchesByCompetitionsOrPredictions(
                    matchParser.getAllMatchesForDate(date), competitionKeys, userPredictionsList);
        } else if (!competitionKeys.isEmpty()) {
            return matchParser.getUserMatches(date, new ArrayList<>(competitionKeys));
        }

        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<TournamentWithStatusResponse> getAllMatchesWithPredictionStatus(String userName, String date) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);
        Set<String> competitionKeys = new HashSet<>(getUserTournamentsByDate(user, targetDate));

        PredictionRequest userPredictionsDTO = predictionService.getUserPredictions(userName, date).orElse(null);
        List<Object> userPredictionsList = MatchParsingUtils.extractUserPredictions(userPredictionsDTO);

        if (userPredictionsDTO != null) {
            competitionKeys.addAll(extractCompetitionKeysFromPredictions(userPredictionsDTO));
        }

        List<Map<String, Object>> userTournaments;
        if (!userPredictionsList.isEmpty()) {
            // Якщо є прогнози — тягнемо всі матчі і фільтруємо по змаганнях + командах прогнозу
            userTournaments = filterMatchesByCompetitionsOrPredictions(
                    matchParser.getAllMatchesForDate(date), competitionKeys, userPredictionsList);
        } else if (!competitionKeys.isEmpty()) {
            userTournaments = matchParser.getUserMatches(date, new ArrayList<>(competitionKeys));
        } else {
            return Collections.emptyList();
        }

        List<Object> allMatchResults = MatchParsingUtils.extractMatchesFromTournaments(userTournaments);
        List<Object> correctPredictions = buildCorrectPredictions(allMatchResults, userPredictionsList);

        return userTournaments.stream()
                .filter(t -> t.get("match") != null)
                .map(t -> TournamentWithStatusResponse.builder()
                        .country((String) t.get("country"))
                        .tournament((String) t.get("tournament"))
                        .matches(buildMatchesWithStatus((List<List<String>>) t.get("match"), correctPredictions))
                        .build())
                .toList();
    }

    // Показуємо турнір якщо: ключ є в обраних змаганнях АБО хоча б один матч збігається з прогнозом
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> filterMatchesByCompetitionsOrPredictions(
            List<Map<String, Object>> allMatches,
            Set<String> competitionKeys,
            List<Object> userPredictions) {
        return allMatches.stream()
                .filter(t -> {
                    String country = (String) t.get("country");
                    String tournament = (String) t.get("tournament");
                    if (country == null || tournament == null) return false;
                    if (competitionKeys.contains(MatchParsingUtils.competitionKey(country, tournament))) return true;
                    Object matchesObj = t.get("match");
                    if (!(matchesObj instanceof List)) return false;
                    List<?> matches = (List<?>) matchesObj;
                    return matches.stream().anyMatch(match ->
                            userPredictions.stream().anyMatch(pred ->
                                    MatchParsingUtils.teamsMatch(match, pred)));
                })
                .collect(Collectors.toList());
    }

    private List<MatchStatusResponse> buildMatchesWithStatus(List<List<String>> matches,
                                                              List<Object> correctPredictions) {
        return matches.stream()
                .map(match -> MatchStatusResponse.builder()
                        .match(match)
                        .isPredictedCorrectly(correctPredictions.stream()
                                .anyMatch(pred -> MatchParsingUtils.matchesAreEqual(pred, match)))
                        .build())
                .toList();
    }

    private List<Object> buildCorrectPredictions(List<Object> allMatchResults, List<Object> userPredictionsList) {
        List<Object> correctPredictions = new ArrayList<>();
        for (Object matchResult : allMatchResults) {
            for (Object userPrediction : userPredictionsList) {
                if (MatchParsingUtils.matchesAreEqual(matchResult, userPrediction)) {
                    correctPredictions.add(matchResult);
                    break;
                }
            }
        }
        return correctPredictions;
    }

    // Групові змагання показуємо тільки якщо targetDate потрапляє в діапазон турніру.
    // Особисті змагання показуємо завжди без обмежень по даті.
    private List<String> getUserTournamentsByDate(User user, LocalDate targetDate) {
        List<String> subscribedTournaments = userCompetitionRepository.findByUser(user).stream()
                .map(uc -> MatchParsingUtils.competitionKey(
                        uc.getCompetition().getCountry(), uc.getCompetition().getName()))
                .toList();

        List<String> groupTournaments = userGroupRepository.findAllGroupsForUser(user).stream()
                .flatMap(userGroup -> groupTournamentRepository.findByUserGroup(userGroup).stream()
                        .filter(t -> t.getStartDate() != null && t.getFinishDate() != null
                                && !targetDate.isBefore(t.getStartDate())
                                && !targetDate.isAfter(t.getFinishDate())))
                .flatMap(tournament -> groupCompetitionRepository.findByGroupTournament(tournament).stream())
                .map(gc -> MatchParsingUtils.competitionKey(
                        gc.getCompetition().getCountry(), gc.getCompetition().getName()))
                .toList();

        Set<String> all = new HashSet<>(subscribedTournaments);
        all.addAll(groupTournaments);
        return new ArrayList<>(all);
    }

    // Витягуємо ключі змагань з прогнозів гравця (об'єкти {country, tournament} в JSON)
    private Set<String> extractCompetitionKeysFromPredictions(PredictionRequest predictions) {
        Set<String> keys = new HashSet<>();
        if (predictions == null || predictions.getPredictions() == null) return keys;

        for (Object item : predictions.getPredictions()) {
            if (item instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) item;
                Object country = map.get("country");
                Object tournament = map.get("tournament");
                if (country instanceof String && tournament instanceof String) {
                    keys.add(MatchParsingUtils.competitionKey((String) country, (String) tournament));
                }
            }
        }
        return keys;
    }
}
