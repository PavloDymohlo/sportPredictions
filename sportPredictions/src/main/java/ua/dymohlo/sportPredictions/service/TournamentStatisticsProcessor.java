package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.component.MatchParser;
import ua.dymohlo.sportPredictions.dto.request.PredictionRequest;
import ua.dymohlo.sportPredictions.entity.GroupTournament;
import ua.dymohlo.sportPredictions.entity.GroupUserStatistics;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.enums.CompetitionStatus;
import ua.dymohlo.sportPredictions.repository.GroupCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.GroupTournamentRepository;
import ua.dymohlo.sportPredictions.repository.GroupUserStatisticsRepository;
import ua.dymohlo.sportPredictions.repository.MatchDataRepository;
import ua.dymohlo.sportPredictions.util.MatchParsingUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TournamentStatisticsProcessor {

    private final GroupCompetitionRepository groupCompetitionRepository;
    private final GroupUserStatisticsRepository groupUserStatisticsRepository;
    private final GroupTournamentRepository groupTournamentRepository;
    private final MatchDataRepository matchDataRepository;
    private final MatchParser matchParser;
    private final PredictionService predictionService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public void process(GroupTournament tournament, LocalDate yesterday, LocalDate predictionTtlLimit) {
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
            calculateStatisticsForDate(tournament, d.format(DATE_FORMATTER));
        }

        tournament.setLastProcessedDate(yesterday);
        groupTournamentRepository.save(tournament);
    }

    private void calculateStatisticsForDate(GroupTournament tournament, String date) {
        log.info("📊 Calculating statistics for tournament id={} in group: {} on date: {}",
                tournament.getId(), tournament.getUserGroup().getGroupName(), date);

        LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);

        if (!isDateInTournamentRange(tournament, targetDate)) return;

        if (tournament.getStatus() != CompetitionStatus.ACTIVE) {
            log.info("⏭️ Tournament {} is not ACTIVE (status: {})", tournament.getId(), tournament.getStatus());
            return;
        }

        List<String> competitionKeys = groupCompetitionRepository.findByGroupTournament(tournament).stream()
                .map(gc -> MatchParsingUtils.competitionKey(
                        gc.getCompetition().getCountry(), gc.getCompetition().getName()))
                .toList();

        if (competitionKeys.isEmpty()) {
            log.warn("⚠️ Tournament {} has no competitions", tournament.getId());
            return;
        }

        List<Map<String, Object>> groupMatches = matchParser.getUserMatches(date, competitionKeys);
        if (groupMatches.isEmpty()) {
            log.warn("⚠️ No matches found for tournament {} on date {}", tournament.getId(), date);
            return;
        }

        List<Object> allMatchResults = MatchParsingUtils.extractMatchesFromTournaments(groupMatches);
        log.info("📊 Found {} matches for tournament competitions", allMatchResults.size());

        for (User member : tournament.getUserGroup().getUsers()) {
            processUserStats(tournament, member, date, allMatchResults);
        }

        updateRankingPositions(tournament);
    }

    private void processUserStats(GroupTournament tournament, User user, String date,
                                   List<Object> allMatchResults) {
        log.info("👤 Processing user {} in tournament {}", user.getUserName(), tournament.getId());

        PredictionRequest userPredictions = predictionService.getUserPredictions(user.getUserName(), date).orElse(null);
        if (userPredictions == null || userPredictions.getPredictions() == null
                || userPredictions.getPredictions().isEmpty()) {
            log.info("⏭️ User {} has no predictions for {}", user.getUserName(), date);
            return;
        }

        List<Object> userPredictionsList = MatchParsingUtils.extractUserPredictions(userPredictions);
        int[] counts = countPredictions(allMatchResults, userPredictionsList);

        if (counts[0] == 0) {
            log.info("⏭️ User {} has no predictions for tournament competitions", user.getUserName());
            return;
        }

        updateUserStats(tournament, user, counts[0], counts[1]);
    }

    private int[] countPredictions(List<Object> allMatchResults, List<Object> userPredictionsList) {
        int total = 0;
        int correct = 0;

        for (Object matchResult : allMatchResults) {
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

    private void updateRankingPositions(GroupTournament tournament) {
        List<GroupUserStatistics> allStats = groupUserStatisticsRepository
                .findByGroupTournamentOrderedByRanking(tournament);

        if (allStats.isEmpty()) return;

        long currentPosition = 1;
        GroupUserStatistics previous = null;

        for (GroupUserStatistics stats : allStats) {
            if (previous != null
                    && stats.getCorrectPredictions() == previous.getCorrectPredictions()
                    && stats.getAccuracyPercent() == previous.getAccuracyPercent()) {
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

    private LocalDate resolveStartDate(GroupTournament tournament, LocalDate predictionTtlLimit) {
        LocalDate lastProcessed = tournament.getLastProcessedDate();
        if (lastProcessed != null) {
            return lastProcessed.plusDays(1);
        }
        return tournament.getStartDate() != null && tournament.getStartDate().isAfter(predictionTtlLimit)
                ? tournament.getStartDate() : predictionTtlLimit;
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
}
