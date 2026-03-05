package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.util.retry.Retry;
import ua.dymohlo.sportPredictions.entity.Competition;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FootballApiService {

    @Value("${football.api.key}")
    private String apiKey;

    @Value("${football.api.token-header}")
    private String tokenHeader;

    private final WebClient footballWebClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int BATCH_SIZE = 10;
    private static final int PAUSE_MILLIS = 62_000;

    public String getCompetitions() {
        log.info("Fetching competitions from Football API");
        return footballWebClient.get()
                .uri("/competitions")
                .header(tokenHeader, apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(10))
                        .filter(this::isRetryable)
                        .doBeforeRetry(signal -> log.warn("Retrying competitions request, attempt {}/3",
                                signal.totalRetries() + 2)))
                .block();
    }

    public List<String> getMatchesFromApi(LocalDate targetDate, List<Competition> competitions) {
        String date = targetDate.format(DATE_FORMATTER);
        log.info("Fetching matches for date: {}", date);

        List<String> allMatchesResponses = new ArrayList<>();

        for (int i = 0; i < competitions.size(); i++) {
            Competition competition = competitions.get(i);
            String code = competition.getCode();

            try {
                String response = footballWebClient.get()
                        .uri("/competitions/{code}/matches?dateFrom={date}&dateTo={date}", code, date, date)
                        .header(tokenHeader, apiKey)
                        .retrieve()
                        .bodyToMono(String.class)
                        .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(10))
                                .filter(this::isRetryable)
                                .doBeforeRetry(signal -> log.warn("Retrying {} attempt {}/3",
                                        code, signal.totalRetries() + 2)))
                        .block();

                if (response != null) {
                    allMatchesResponses.add(response);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch matches for competition {} after all retries: {}", code, e.getMessage());
            }

            pauseIfBatchLimitReached(i + 1, competitions.size());
        }

        if (allMatchesResponses.isEmpty() && !competitions.isEmpty()) {
            throw new RuntimeException("All competition match requests failed for date: " + date);
        }

        log.info("Fetched matches from {} competitions", allMatchesResponses.size());
        return allMatchesResponses;
    }

    private void pauseIfBatchLimitReached(int processed, int total) {
        if (processed % BATCH_SIZE == 0 && processed < total) {
            log.info("Batch limit reached ({} requests). Sleeping {} ms...", BATCH_SIZE, PAUSE_MILLIS);
            try {
                Thread.sleep(PAUSE_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Sleep interrupted");
            }
        }
    }

    private boolean isRetryable(Throwable t) {
        return t instanceof WebClientRequestException;
    }
}