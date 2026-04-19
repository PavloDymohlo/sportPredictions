package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ua.dymohlo.sportPredictions.component.ApiDataParser;
import ua.dymohlo.sportPredictions.entity.MatchData;
import ua.dymohlo.sportPredictions.repository.CompetitionRepository;
import ua.dymohlo.sportPredictions.repository.MatchDataRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchDataCacheService {

    private final ApiDataParser apiDataParser;
    private final FootballApiService footballApiService;
    private final CompetitionRepository competitionRepository;
    private final MatchDataRepository matchDataRepository;
    private final TaskScheduler taskScheduler;

    private static final int MATCH_DATA_TTL_DAYS = 4;
    private static final int RETRY_DELAY_MINUTES = 30;

    public String parseAndCachePastMatches(Runnable onRetrySuccess) {
        return parseAndCacheMatchesInternal(LocalDate.now().minusDays(1), false, onRetrySuccess);
    }

        public String parseAndCacheFutureMatches() {
        return parseAndCacheMatchesInternal(LocalDate.now().plusDays(3), false, null);
    }


    public String parseAndCacheMatchesFromApi(LocalDate date) {
        return parseAndCacheMatchesInternal(date, false, null);
    }

    private String parseAndCacheMatchesInternal(LocalDate date, boolean isRetry, Runnable onSuccess) {
        try {
            List<String> responses = footballApiService.getMatchesFromApi(date, competitionRepository.findAll());
            String parsedData = apiDataParser.parseMatches(responses, date);

            LocalDate cutoff = LocalDate.now().minusDays(MATCH_DATA_TTL_DAYS);
            if (date.isBefore(cutoff)) {
                log.info("Match date {} is older than 4 days, skipping storage", date);
                return parsedData;
            }

            MatchData matchData = matchDataRepository.findByMatchDate(date)
                    .orElse(MatchData.builder().matchDate(date).build());
            matchData.setMatchesJson(parsedData);
            matchData.setFetchedAt(LocalDateTime.now());
            matchDataRepository.save(matchData);

            log.info("Match data saved to DB for date: {}", date);

            if (isRetry && onSuccess != null) {
                log.info("Retry succeeded for date {} — running post-fetch operations", date);
                try {
                    onSuccess.run();
                } catch (Exception e) {
                    log.error("Post-fetch operations failed after retry for date {}", date, e);
                }
            }

            return parsedData;
        } catch (Exception e) {
            log.error("Error updating match data for date: {}", date, e);
            if (!isRetry) {
                scheduleRetry(() -> parseAndCacheMatchesInternal(date, true, onSuccess), "matches for " + date);
            } else {
                log.error("Match data retry for date {} also failed. Giving up.", date);
            }
            throw new RuntimeException("Failed to fetch match data for date: " + date, e);
        }
    }

    public void cleanupOldMatchData() {
        LocalDate cutoff = LocalDate.now().minusDays(MATCH_DATA_TTL_DAYS);
        matchDataRepository.deleteByMatchDateBefore(cutoff);
        log.info("Deleted match data older than {}", cutoff);
    }

    private void scheduleRetry(Runnable task, String description) {
        Instant retryTime = Instant.now().plus(RETRY_DELAY_MINUTES, ChronoUnit.MINUTES);
        log.warn("Scheduling retry for '{}' at {}", description, retryTime);
        taskScheduler.schedule(task, retryTime);
    }
}
