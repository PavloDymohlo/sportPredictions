package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.repository.CompetitionRepository;

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

    @Value("${football.api.base-url}")
    private String baseUrl;

    @Value("${football.api.token-header}")
    private String tokenHeader;
    private final CompetitionRepository competitionRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ResponseEntity<String> getCompetitions() {
        log.info("Fetching competitions from Football API");
        HttpHeaders headers = new HttpHeaders();
        headers.set(tokenHeader, apiKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/competitions",
                HttpMethod.GET,
                entity,
                String.class
        );

        System.out.println("Available competitions: " + response.getBody());

        return response;
    }

    public ResponseEntity<String> getFutureMatches() {
        LocalDate futureDate = LocalDate.now().plusDays(3);
        String date = futureDate.format(DATE_FORMATTER);

        log.info("Fetching upcoming matches for date: {}", date);

        HttpHeaders headers = new HttpHeaders();
        headers.set(tokenHeader, apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = String.format("%s/matches?dateFrom=%s&dateTo=%s", baseUrl, date, date);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
        );

        System.out.println("Future matches: " + response.getBody());

        return response;
    }

    public ResponseEntity<List<String>> getMatchesFromApi(LocalDate targetDate) {
        String date = targetDate.format(DATE_FORMATTER);

        log.info("Fetching past matches for date: {}", date);

        HttpHeaders headers = new HttpHeaders();
        headers.set(tokenHeader, apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<String> allMatchesResponses = new ArrayList<>();
        List<Competition> competitions = competitionRepository.findAll();

        int batchSize = 10;
        int pauseMillis = 62_000;

        for (int i = 0; i < competitions.size(); i++) {

            Competition competition = competitions.get(i);
            String code = competition.getCode();

            String url = String.format(
                    "%s/competitions/%s/matches?dateFrom=%s&dateTo=%s",
                    baseUrl, code, date, date
            );

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getBody() != null) {
                    allMatchesResponses.add(response.getBody());
                }

            } catch (Exception e) {
                log.warn("Failed to fetch matches for competition {}: {}", code, e.getMessage());
            }

            if ((i + 1) % batchSize == 0 && (i + 1) < competitions.size()) {
                log.info("Batch limit reached ({} requests). Sleeping {} ms...", batchSize, pauseMillis);
                try {
                    Thread.sleep(pauseMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Sleep interrupted");
                }
            }
        }

        log.info("Fetched matches from {} competitions", allMatchesResponses.size());
        return ResponseEntity.ok(allMatchesResponses);
    }
}
