package ua.dymohlo.sportPredictions.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.dymohlo.sportPredictions.component.ApiDataParser;
import ua.dymohlo.sportPredictions.service.PredictionService;


@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateDataSchedule {
    private final ApiDataParser apiDataParser;
    private final PredictionService predictionService;

    @Scheduled(cron = "00 14 12 * * *", zone = "Europe/Kiev")
    public void getCompetitionFromApi() {
        log.info("call footballApiService");
        apiDataParser.parseCompetitionsData();
    }

    @Scheduled(cron = "30 14 12 * * *", zone = "Europe/Kiev")
    public void getPastMatches() {
        log.info("call footballApiService");
        apiDataParser.parseAndCachePastMatches();
    }

    @Scheduled(cron = "0 17 12 * * *", zone = "Europe/Kiev")
    public void getFutureMatches() {
        log.info("call footballApiService");
        apiDataParser.parseAndCacheFutureMatches();
    }

    @Scheduled(cron = "0 19 12 * * *", zone = "Europe/Kiev")
    public void countUsersPredictionsResult() {
        predictionService.countAllUsersPredictionsResult();
    }
}
