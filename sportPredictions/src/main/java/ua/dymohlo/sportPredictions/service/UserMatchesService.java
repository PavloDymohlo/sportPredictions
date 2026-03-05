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
import ua.dymohlo.sportPredictions.enums.CompetitionStatus;
import ua.dymohlo.sportPredictions.repository.GroupCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.GroupTournamentRepository;
import ua.dymohlo.sportPredictions.repository.UserCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.UserGroupRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;
import ua.dymohlo.sportPredictions.util.MatchParsingUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserFutureMatches(String userName, String date) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> allTournaments = getUserTournaments(user);

        if (allTournaments.isEmpty()) {
            return Collections.emptyList();
        }

        return matchParser.getUserMatches(date, allTournaments);
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public List<TournamentWithStatusResponse> getAllMatchesWithPredictionStatus(String userName, String date) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> allTournaments = getUserTournaments(user);

        if (allTournaments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> userTournaments = matchParser.getUserMatches(date, allTournaments);
        PredictionRequest userPredictionsDTO = predictionService.getUserPredictions(userName, date).orElse(null);

        List<Object> allMatchResults = MatchParsingUtils.extractMatchesFromTournaments(userTournaments);
        List<Object> userPredictionsList = MatchParsingUtils.extractUserPredictions(userPredictionsDTO);

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

    private List<String> getUserTournaments(User user) {
        List<String> subscribedTournaments = userCompetitionRepository.findByUser(user).stream()
                .map(uc -> MatchParsingUtils.competitionKey(
                        uc.getCompetition().getCountry(), uc.getCompetition().getName()))
                .toList();

        List<String> groupTournaments = userGroupRepository.findAllGroupsForUser(user).stream()
                .flatMap(userGroup -> groupTournamentRepository.findByUserGroup(userGroup).stream()
                        .filter(t -> t.getStatus() == CompetitionStatus.ACTIVE
                                || t.getStatus() == CompetitionStatus.NOT_STARTED))
                .flatMap(tournament -> groupCompetitionRepository.findByGroupTournament(tournament).stream())
                .map(gc -> MatchParsingUtils.competitionKey(
                        gc.getCompetition().getCountry(), gc.getCompetition().getName()))
                .toList();

        Set<String> allTournaments = new HashSet<>(subscribedTournaments);
        allTournaments.addAll(groupTournaments);

        return new ArrayList<>(allTournaments);
    }
}
