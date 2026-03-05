package ua.dymohlo.sportPredictions.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.dymohlo.sportPredictions.service.CompetitionCacheService;
import ua.dymohlo.sportPredictions.service.GroupStatisticsService;
import ua.dymohlo.sportPredictions.service.MatchDataCacheService;
import ua.dymohlo.sportPredictions.service.PredictionResultService;
import ua.dymohlo.sportPredictions.service.PredictionService;
import ua.dymohlo.sportPredictions.service.SchedulerStatusService;
import ua.dymohlo.sportPredictions.service.TelegramNotificationService;
import ua.dymohlo.sportPredictions.service.TournamentLifecycleService;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateDataSchedule {

    private final CompetitionCacheService competitionCacheService;
    private final MatchDataCacheService matchDataCacheService;
    private final PredictionResultService predictionResultService;
    private final PredictionService predictionService;
    private final GroupStatisticsService groupStatisticsService;
    private final TournamentLifecycleService tournamentLifecycleService;
    private final SchedulerStatusService schedulerStatusService;
    private final TelegramNotificationService telegramNotificationService;

    @Scheduled(cron = "${scheduler.cron}", zone = "Europe/Kiev")
    public void runDailyUpdate() {
        log.info("Daily update started");
        schedulerStatusService.setRunning();

        boolean competitionsOk = false, pastMatchesOk = false, futureMatchesOk = false, statsOk = false;

        try {
            competitionCacheService.parseCompetitionsData();
            competitionsOk = true;
        } catch (Exception e) {
            log.error("Failed to fetch competitions from API", e);
        }

        try {
            Runnable onRetrySuccess = () -> {
                predictionResultService.countAllUsersPredictionsResult();
                groupStatisticsService.calculateAllGroupsStatistics();
                tournamentLifecycleService.finalizeCompletedTournaments();
                schedulerStatusService.setCompleted();
            };
            matchDataCacheService.parseAndCachePastMatches(onRetrySuccess);
            pastMatchesOk = true;
        } catch (Exception e) {
            log.error("Failed to fetch and cache past matches", e);
        }

        try {
            matchDataCacheService.parseAndCacheFutureMatches();
            futureMatchesOk = true;
        } catch (Exception e) {
            log.error("Failed to fetch and cache future matches", e);
        }

        try {
            if (pastMatchesOk) {
                predictionResultService.countAllUsersPredictionsResult();
            } else {
                log.warn("Skipping prediction scoring — past match data not available");
            }
            groupStatisticsService.calculateAllGroupsStatistics();
            tournamentLifecycleService.finalizeCompletedTournaments();
            tournamentLifecycleService.deleteExpiredFinishedTournaments();
            statsOk = pastMatchesOk;
        } catch (Exception e) {
            log.error("Failed to calculate group statistics", e);
        }

        try {
            predictionService.cleanupOldPredictions();
            matchDataCacheService.cleanupOldMatchData();
        } catch (Exception e) {
            log.error("Failed to cleanup old data", e);
        }

        if (competitionsOk && pastMatchesOk && futureMatchesOk && statsOk) {
            schedulerStatusService.setCompleted();
            log.info("Daily update finished successfully");
            telegramNotificationService.notifyAllUsers();
        } else {
            schedulerStatusService.setIncomplete();
            log.warn("Daily update finished with errors (competitions={}, pastMatches={}, futureMatches={}, stats={})",
                    competitionsOk, pastMatchesOk, futureMatchesOk, statsOk);
        }
    }
}
