package ua.dymohlo.sportPredictions.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import ua.dymohlo.sportPredictions.component.ApiDataParser;
import ua.dymohlo.sportPredictions.dto.response.CompetitionResponse;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.repository.CompetitionRepository;
import ua.dymohlo.sportPredictions.repository.GroupCompetitionRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompetitionCacheService {

    private static final int RETRY_DELAY_MINUTES = 30;

    private final ApiDataParser apiDataParser;
    private final FootballApiService footballApiService;
    private final CompetitionRepository competitionRepository;
    private final GroupCompetitionRepository groupCompetitionRepository;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;

    public String parseCompetitionsData() {
        return parseCompetitionsDataInternal(false);
    }

    private String parseCompetitionsDataInternal(boolean isRetry) {
        try {
            String parsedData = apiDataParser.parseCompetitions(footballApiService.getCompetitions());
            syncCompetitionsWithDb(parsedData);
            log.info("Competitions synced with DB");
            return parsedData;
        } catch (Exception e) {
            log.error("Error updating competitions", e);
            if (!isRetry) {
                scheduleRetry(() -> parseCompetitionsDataInternal(true), "competitions");
            } else {
                log.error("Competitions retry also failed. Giving up.");
            }
            return "[]";
        }
    }

    private void syncCompetitionsWithDb(String parsedData) {
        try {
            List<Map<String, Object>> apiCompetitions = objectMapper.readValue(
                    parsedData, new TypeReference<List<Map<String, Object>>>() {});

            List<String> apiCodes = apiCompetitions.stream()
                    .map(m -> (String) m.get("code"))
                    .filter(Objects::nonNull)
                    .toList();

            if (apiCodes.isEmpty()) {
                log.warn("API returned empty competitions list — skipping DB sync to avoid data loss");
                return;
            }

            List<Competition> obsolete = competitionRepository.findByCodeNotIn(apiCodes);
            for (Competition competition : obsolete) {
                groupCompetitionRepository.deleteByCompetition(competition);
                competitionRepository.delete(competition);
                log.warn("Removed competition '{}' ({}) — no longer available in API subscription",
                        competition.getName(), competition.getCode());
            }

            if (!obsolete.isEmpty()) {
                log.info("DB sync complete: removed {} obsolete competition(s)", obsolete.size());
            }
        } catch (Exception e) {
            log.error("Error during competitions DB sync", e);
        }
    }

    public List<CompetitionResponse> getCompetitionsList() {
        List<Competition> competitions = competitionRepository.findAll();
        if (competitions.isEmpty()) {
            log.info("No competitions in DB, fetching from API");
            parseCompetitionsData();
            competitions = competitionRepository.findAll();
        }
        log.info("Retrieved {} competitions from DB", competitions.size());
        return competitions.stream()
                .map(c -> CompetitionResponse.builder()
                        .id(c.getId())
                        .country(c.getCountry())
                        .name(c.getName())
                        .code(c.getCode())
                        .build())
                .toList();
    }

    private void scheduleRetry(Runnable task, String description) {
        Instant retryTime = Instant.now().plus(RETRY_DELAY_MINUTES, ChronoUnit.MINUTES);
        log.warn("⏰ Scheduling retry for '{}' at {}", description, retryTime);
        taskScheduler.schedule(task, retryTime);
    }
}
