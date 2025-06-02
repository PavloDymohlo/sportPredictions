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

    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> getCompetitions() {
        log.info("Fetching competitions from Football API");
        HttpHeaders headers = new HttpHeaders();
        headers.set(tokenHeader, apiKey);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                baseUrl + "/competitions",
                HttpMethod.GET,
                entity,
                String.class
        );
    }
}

