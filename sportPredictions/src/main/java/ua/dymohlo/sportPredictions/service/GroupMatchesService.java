package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.component.MatchParser;
import ua.dymohlo.sportPredictions.dto.request.PredictionRequest;
import ua.dymohlo.sportPredictions.dto.response.GroupMatchesWithPredictionsResponse;
import ua.dymohlo.sportPredictions.dto.response.GroupMatchesWithPredictionsResponse.CompetitionMatches;
import ua.dymohlo.sportPredictions.dto.response.GroupMatchesWithPredictionsResponse.MatchWithPredictions;
import ua.dymohlo.sportPredictions.dto.response.UserPrediction;
import ua.dymohlo.sportPredictions.entity.GroupTournament;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserGroup;
import ua.dymohlo.sportPredictions.repository.GroupCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.GroupTournamentRepository;
import ua.dymohlo.sportPredictions.repository.UserGroupRepository;
import ua.dymohlo.sportPredictions.util.MatchParsingUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupMatchesService {

    private final UserGroupRepository userGroupRepository;
    private final GroupCompetitionRepository groupCompetitionRepository;
    private final GroupTournamentRepository groupTournamentRepository;
    private final MatchParser matchParser;
    private final PredictionService predictionService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public GroupMatchesWithPredictionsResponse getGroupMatchesWithPredictions(String groupName, String date, Long tournamentId) {
        UserGroup group = userGroupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);
        GroupTournament tournament = resolveTournament(group, targetDate, tournamentId);

        if (tournament == null) {
            return emptyMatchesResponse(groupName, date);
        }

        List<String> tournamentCompetitions = loadTournamentCompetitionKeys(tournament);
        if (tournamentCompetitions.isEmpty()) {
            return emptyMatchesResponse(groupName, date);
        }

        List<Map<String, Object>> groupMatches = matchParser.getUserMatches(date, tournamentCompetitions);
        if (groupMatches.isEmpty()) {
            return emptyMatchesResponse(groupName, date);
        }

        List<String> members = group.getUsers().stream()
                .map(User::getUserName)
                .toList();

        Map<String, PredictionRequest> userPredictions = loadUserPredictions(members, date);
        List<CompetitionMatches> competitions = buildCompetitionMatches(groupMatches, members, userPredictions);

        return GroupMatchesWithPredictionsResponse.builder()
                .groupName(groupName)
                .date(date)
                .members(members)
                .competitions(competitions)
                .build();
    }

    private GroupTournament resolveTournament(UserGroup group, LocalDate date, Long tournamentId) {
        if (tournamentId != null) {
            return groupTournamentRepository.findById(tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        }
        return groupTournamentRepository.findByUserGroupAndDateInRange(group, date).orElse(null);
    }

    private List<String> loadTournamentCompetitionKeys(GroupTournament tournament) {
        return groupCompetitionRepository.findByGroupTournament(tournament).stream()
                .map(gc -> MatchParsingUtils.competitionKey(
                        gc.getCompetition().getCountry(), gc.getCompetition().getName()))
                .toList();
    }

    private Map<String, PredictionRequest> loadUserPredictions(List<String> members, String date) {
        Map<String, PredictionRequest> userPredictions = new HashMap<>();
        for (String userName : members) {
            predictionService.getUserPredictions(userName, date)
                    .ifPresent(p -> userPredictions.put(userName, p));
        }
        return userPredictions;
    }

    private List<CompetitionMatches> buildCompetitionMatches(List<Map<String, Object>> groupMatches,
                                                             List<String> members,
                                                             Map<String, PredictionRequest> userPredictions) {
        List<CompetitionMatches> competitions = new ArrayList<>();
        for (Map<String, Object> tournamentData : groupMatches) {
            String country = (String) tournamentData.get("country");
            String tournamentName = (String) tournamentData.get("tournament");
            List<List<String>> matches = (List<List<String>>) tournamentData.get("match");

            if (matches == null || matches.isEmpty()) continue;

            competitions.add(CompetitionMatches.builder()
                    .country(country)
                    .tournament(tournamentName)
                    .matches(buildMatchesWithPredictions(matches, members, userPredictions))
                    .build());
        }
        return competitions;
    }

    private List<MatchWithPredictions> buildMatchesWithPredictions(List<List<String>> matches,
                                                                    List<String> members,
                                                                    Map<String, PredictionRequest> userPredictions) {
        List<MatchWithPredictions> result = new ArrayList<>();
        for (List<String> match : matches) {
            if (match.size() != 2) continue;

            String homeTeamResult = match.get(0);
            String awayTeamResult = match.get(1);

            String homeTeam = MatchParsingUtils.extractTeamName(homeTeamResult);
            String awayTeam = MatchParsingUtils.extractTeamName(awayTeamResult);
            String homeScore = MatchParsingUtils.extractScoreString(homeTeamResult);
            String awayScore = MatchParsingUtils.extractScoreString(awayTeamResult);

            result.add(MatchWithPredictions.builder()
                    .homeTeam(homeTeam)
                    .awayTeam(awayTeam)
                    .homeScore(homeScore)
                    .awayScore(awayScore)
                    .predictions(buildUserPredictions(members, userPredictions, homeTeam, awayTeam, homeScore, awayScore))
                    .build());
        }
        return result;
    }

    private Map<String, UserPrediction> buildUserPredictions(List<String> members,
                                                              Map<String, PredictionRequest> userPredictions,
                                                              String homeTeam, String awayTeam,
                                                              String homeScore, String awayScore) {
        Map<String, UserPrediction> predictions = new HashMap<>();
        for (String userName : members) {
            PredictionRequest userPred = userPredictions.get(userName);
            if (userPred == null || userPred.getPredictions() == null) {
                predictions.put(userName, emptyPrediction());
            } else {
                predictions.put(userName, findPredictionForMatch(userPred, homeTeam, awayTeam, homeScore, awayScore));
            }
        }
        return predictions;
    }

    private UserPrediction findPredictionForMatch(PredictionRequest userPred,
                                                   String homeTeam, String awayTeam,
                                                   String homeScore, String awayScore) {
        List<Object> predictions = MatchParsingUtils.extractUserPredictions(userPred);
        for (Object pred : predictions) {
            if (!(pred instanceof List)) continue;
            List<?> match = (List<?>) pred;
            if (match.size() != 2) continue;

            String homePred = String.valueOf(match.get(0));
            String awayPred = String.valueOf(match.get(1));

            if (MatchParsingUtils.extractTeamName(homePred).equalsIgnoreCase(homeTeam) &&
                    MatchParsingUtils.extractTeamName(awayPred).equalsIgnoreCase(awayTeam)) {

                String homePredScore = MatchParsingUtils.extractScoreString(homePred);
                String awayPredScore = MatchParsingUtils.extractScoreString(awayPred);

                return UserPrediction.builder()
                        .homePrediction(homePredScore)
                        .awayPrediction(awayPredScore)
                        .isCorrect(homePredScore.equals(homeScore) && awayPredScore.equals(awayScore))
                        .build();
            }
        }
        return emptyPrediction();
    }

    private UserPrediction emptyPrediction() {
        return UserPrediction.builder()
                .homePrediction("-")
                .awayPrediction("-")
                .isCorrect(false)
                .build();
    }

    private GroupMatchesWithPredictionsResponse emptyMatchesResponse(String groupName, String date) {
        return GroupMatchesWithPredictionsResponse.builder()
                .groupName(groupName)
                .date(date)
                .members(List.of())
                .competitions(List.of())
                .build();
    }
}
