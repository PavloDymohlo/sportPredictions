package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.component.MatchParser;
import ua.dymohlo.sportPredictions.dto.request.PredictionRequest;
import ua.dymohlo.sportPredictions.dto.response.GroupRankingResponse;
import ua.dymohlo.sportPredictions.entity.GroupTournament;
import ua.dymohlo.sportPredictions.entity.GroupUserStatistics;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserGroup;
import ua.dymohlo.sportPredictions.enums.CompetitionStatus;
import ua.dymohlo.sportPredictions.repository.GroupCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.GroupTournamentRepository;
import ua.dymohlo.sportPredictions.repository.GroupUserStatisticsRepository;
import ua.dymohlo.sportPredictions.repository.MatchDataRepository;
import ua.dymohlo.sportPredictions.repository.UserGroupRepository;
import ua.dymohlo.sportPredictions.util.MatchParsingUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupStatisticsService {

    private final UserGroupRepository userGroupRepository;
    private final GroupCompetitionRepository groupCompetitionRepository;
    private final GroupUserStatisticsRepository groupUserStatisticsRepository;
    private final GroupTournamentRepository groupTournamentRepository;
    private final MatchDataRepository matchDataRepository;
    private final MatchParser matchParser;
    private final PredictionService predictionService;
    private final TournamentLifecycleService tournamentLifecycleService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public void calculateAllGroupsStatistics() {
        log.info("🎯 ========== STARTING GROUP STATISTICS CALCULATION ==========");

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate predictionTtlLimit = today.minusDays(3);

        log.info("📅 Processing up to: {}", yesterday);
        tournamentLifecycleService.updateTournamentStatuses();

        List<GroupTournament> activeTournaments = groupTournamentRepository.findByStatus(CompetitionStatus.ACTIVE);
        log.info("🏆 Found {} ACTIVE tournaments to process", activeTournaments.size());

        for (GroupTournament tournament : activeTournaments) {
            try {
                processTournament(tournament, yesterday, predictionTtlLimit);
            } catch (Exception e) {
                log.error("❌ Error processing tournament {}: {}", tournament.getId(), e.getMessage(), e);
            }
        }

        log.info("🎉 ========== GROUP STATISTICS CALCULATION COMPLETE ==========");
    }

    private void processTournament(GroupTournament tournament, LocalDate yesterday, LocalDate predictionTtlLimit) {
        LocalDate processFrom = resolveStartDate(tournament, predictionTtlLimit);

        if (processFrom.isAfter(yesterday)) {
            log.info("⏭️ Tournament {} is up to date (lastProcessed={})",
                    tournament.getId(), tournament.getLastProcessedDate());
            return;
        }

        log.info("📊 Tournament {} — processing dates {} to {}", tournament.getId(), processFrom, yesterday);

        for (LocalDate d = processFrom; !d.isAfter(yesterday); d = d.plusDays(1)) {
            if (!matchDataRepository.existsByMatchDate(d)) {
                log.warn("⚠️ No match data for {} — pausing stats for tournament {}, will retry next run",
                        d, tournament.getId());
                return;
            }
            calculateTournamentStatisticsForDate(tournament, d.format(DATE_FORMATTER));
        }

        tournament.setLastProcessedDate(yesterday);
        groupTournamentRepository.save(tournament);
    }

    private LocalDate resolveStartDate(GroupTournament tournament, LocalDate predictionTtlLimit) {
        LocalDate lastProcessed = tournament.getLastProcessedDate();
        if (lastProcessed != null) {
            return lastProcessed.plusDays(1);
        }
        return tournament.getStartDate() != null && tournament.getStartDate().isAfter(predictionTtlLimit)
                ? tournament.getStartDate() : predictionTtlLimit;
    }

    public void calculateTournamentStatisticsForDate(GroupTournament tournament, String date) {
        log.info("📊 Calculating statistics for tournament id={} in group: {} on date: {}",
                tournament.getId(), tournament.getUserGroup().getGroupName(), date);

        LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);

        if (!isDateInTournamentRange(tournament, targetDate)) return;

        if (tournament.getStatus() != CompetitionStatus.ACTIVE) {
            log.info("⏭️ Tournament {} is not ACTIVE (status: {})", tournament.getId(), tournament.getStatus());
            return;
        }

        List<String> tournamentCompetitions = loadTournamentCompetitionKeys(tournament);
        if (tournamentCompetitions.isEmpty()) {
            log.warn("⚠️ Tournament {} has no competitions", tournament.getId());
            return;
        }

        List<Map<String, Object>> groupMatches = matchParser.getUserMatches(date, tournamentCompetitions);
        if (groupMatches.isEmpty()) {
            log.warn("⚠️ No matches found for tournament {} on date {}", tournament.getId(), date);
            return;
        }

        List<Object> allGroupMatchResults = MatchParsingUtils.extractMatchesFromTournaments(groupMatches);
        log.info("📊 Found {} matches for tournament competitions", allGroupMatchResults.size());

        for (User member : tournament.getUserGroup().getUsers()) {
            processUserStatisticsInTournament(tournament, member, date, allGroupMatchResults);
        }

        updateTournamentRankingPositions(tournament);
    }

    private boolean isDateInTournamentRange(GroupTournament tournament, LocalDate date) {
        if (tournament.getStartDate() != null && date.isBefore(tournament.getStartDate())) {
            log.info("⏭️ Date {} is before tournament start date {}", date, tournament.getStartDate());
            return false;
        }
        if (tournament.getFinishDate() != null && date.isAfter(tournament.getFinishDate())) {
            log.info("⏭️ Date {} is after tournament finish date {}", date, tournament.getFinishDate());
            return false;
        }
        return true;
    }

    private List<String> loadTournamentCompetitionKeys(GroupTournament tournament) {
        return groupCompetitionRepository.findByGroupTournament(tournament).stream()
                .map(gc -> MatchParsingUtils.competitionKey(
                        gc.getCompetition().getCountry(), gc.getCompetition().getName()))
                .toList();
    }

    private void processUserStatisticsInTournament(GroupTournament tournament, User user, String date,
                                                    List<Object> allGroupMatchResults) {
        log.info("👤 Processing user {} in tournament {}", user.getUserName(), tournament.getId());

        PredictionRequest userPredictions = predictionService.getUserPredictions(user.getUserName(), date).orElse(null);
        if (userPredictions == null || userPredictions.getPredictions() == null ||
                userPredictions.getPredictions().isEmpty()) {
            log.info("⏭️ User {} has no predictions for {}", user.getUserName(), date);
            return;
        }

        List<Object> userPredictionsList = MatchParsingUtils.extractUserPredictions(userPredictions);
        int[] counts = countPredictions(allGroupMatchResults, userPredictionsList);

        if (counts[0] == 0) {
            log.info("⏭️ User {} has no predictions for tournament competitions", user.getUserName());
            return;
        }

        updateUserStats(tournament, user, counts[0], counts[1]);
    }

    private int[] countPredictions(List<Object> allGroupMatchResults, List<Object> userPredictionsList) {
        int total = 0;
        int correct = 0;

        for (Object matchResult : allGroupMatchResults) {
            for (Object userPrediction : userPredictionsList) {
                if (MatchParsingUtils.teamsMatch(matchResult, userPrediction)) {
                    total++;
                    if (MatchParsingUtils.matchesAreEqual(matchResult, userPrediction)) {
                        correct++;
                    }
                    break;
                }
            }
        }

        return new int[]{total, correct};
    }

    private void updateUserStats(GroupTournament tournament, User user, int predictionCount, int correctCount) {
        GroupUserStatistics stats = groupUserStatisticsRepository
                .findByGroupTournamentAndUser(tournament, user)
                .orElse(GroupUserStatistics.builder()
                        .groupTournament(tournament)
                        .user(user)
                        .correctPredictions(0L)
                        .predictionCount(0L)
                        .accuracyPercent(0)
                        .rankingPosition(0L)
                        .build());

        stats.setCorrectPredictions(stats.getCorrectPredictions() + correctCount);
        stats.setPredictionCount(stats.getPredictionCount() + predictionCount);
        stats.setAccuracyPercent(MatchParsingUtils.calculateAccuracyPercent(
                stats.getCorrectPredictions(), stats.getPredictionCount()));

        groupUserStatisticsRepository.save(stats);

        log.info("✅ Updated stats for {} in tournament {}: correct={}/{}, accuracy={}%",
                user.getUserName(), tournament.getId(),
                stats.getCorrectPredictions(), stats.getPredictionCount(), stats.getAccuracyPercent());
    }

    private void updateTournamentRankingPositions(GroupTournament tournament) {
        List<GroupUserStatistics> allStats = groupUserStatisticsRepository
                .findByGroupTournamentOrderedByRanking(tournament);

        if (allStats.isEmpty()) return;

        long currentPosition = 1;
        GroupUserStatistics previous = null;

        for (GroupUserStatistics stats : allStats) {
            if (previous != null &&
                    stats.getCorrectPredictions() == previous.getCorrectPredictions() &&
                    stats.getAccuracyPercent() == previous.getAccuracyPercent()) {
                stats.setRankingPosition(previous.getRankingPosition());
            } else {
                stats.setRankingPosition(currentPosition);
            }
            previous = stats;
            currentPosition++;
        }

        groupUserStatisticsRepository.saveAll(allStats);
        log.info("✅ Updated ranking positions for tournament {}", tournament.getId());
    }

    @Transactional(readOnly = true)
    public List<GroupRankingResponse> getGroupRanking(String groupName, Long tournamentId) {
        UserGroup group = userGroupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        GroupTournament tournament = resolveTournament(group, tournamentId);
        if (tournament == null) {
            return buildEmptyRanking(group);
        }

        List<GroupUserStatistics> stats = groupUserStatisticsRepository
                .findByGroupTournamentOrderedByRanking(tournament);

        Set<Long> usersWithStats = stats.stream()
                .map(s -> s.getUser().getId())
                .collect(Collectors.toSet());

        List<GroupRankingResponse> result = stats.stream()
                .map(s -> GroupRankingResponse.builder()
                        .rankingPosition(s.getRankingPosition())
                        .userName(s.getUser().getUserName())
                        .correctPredictions(s.getCorrectPredictions())
                        .predictionCount(s.getPredictionCount())
                        .accuracyPercent(s.getAccuracyPercent())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        if (group.getUsers() != null) {
            long nextPosition = result.isEmpty() ? 1 : result.size() + 1;
            for (User member : group.getUsers()) {
                if (!usersWithStats.contains(member.getId())) {
                    result.add(GroupRankingResponse.builder()
                            .rankingPosition(nextPosition++)
                            .userName(member.getUserName())
                            .correctPredictions(0L)
                            .predictionCount(0L)
                            .accuracyPercent(0)
                            .build());
                }
            }
        }

        return result;
    }

    private GroupTournament resolveTournament(UserGroup group, Long tournamentId) {
        if (tournamentId != null) {
            return groupTournamentRepository.findById(tournamentId)
                    .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
        }

        List<GroupTournament> tournaments = groupTournamentRepository.findByUserGroup(group);
        return tournaments.stream()
                .filter(t -> t.getStatus() == CompetitionStatus.ACTIVE)
                .findFirst()
                .orElseGet(() -> tournaments.stream()
                        .filter(t -> t.getStatus() == CompetitionStatus.FINISHED)
                        .max(Comparator.comparing(GroupTournament::getFinishDate))
                        .orElse(null));
    }

    private List<GroupRankingResponse> buildEmptyRanking(UserGroup group) {
        if (group.getUsers() == null) return List.of();
        long position = 1;
        List<GroupRankingResponse> result = new ArrayList<>();
        for (User member : group.getUsers()) {
            result.add(GroupRankingResponse.builder()
                    .rankingPosition(position++)
                    .userName(member.getUserName())
                    .correctPredictions(0L)
                    .predictionCount(0L)
                    .accuracyPercent(0)
                    .build());
        }
        return result;
    }
}
