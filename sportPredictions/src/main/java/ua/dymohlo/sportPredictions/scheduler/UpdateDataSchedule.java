package ua.dymohlo.sportPredictions.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private TelegramNotificationService telegramNotificationService;

    @Scheduled(cron = "${scheduler.telegram-cron}", zone = "Europe/Kiev")
    public void sendTelegramNotifications() {
        if (telegramNotificationService == null) {
            log.info("Telegram disabled — skipping notifications");
            return;
        }
        log.info("Sending Telegram notifications");
        telegramNotificationService.notifyAllUsers();
    }

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

        Runnable postFetchOperations = () -> {
            predictionResultService.countAllUsersPredictionsResult();
            groupStatisticsService.calculateAllGroupsStatistics();
            tournamentLifecycleService.finalizeCompletedTournaments();
            tournamentLifecycleService.deleteExpiredFinishedTournaments();
        };

        try {
            matchDataCacheService.parseAndCachePastMatches(postFetchOperations);
            pastMatchesOk = true;
        } catch (Exception e) {
            log.error("Failed to fetch and cache past matches — stats will run after retry", e);
        }

        try {
            matchDataCacheService.parseAndCacheFutureMatches();
            futureMatchesOk = true;
        } catch (Exception e) {
            log.error("Failed to fetch and cache future matches", e);
        }

        try {
            if (pastMatchesOk) {
                postFetchOperations.run();
            } else {
                log.warn("Skipping statistics — past match data not available, retry scheduled");
            }
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
        } else {
            schedulerStatusService.setIncomplete();
            log.warn("Daily update finished with errors (competitions={}, pastMatches={}, futureMatches={}, stats={})",
                    competitionsOk, pastMatchesOk, futureMatchesOk, statsOk);
        }
    }
}
