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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompetitionCacheService {

    private static final int RETRY_DELAY_MINUTES = 30;

    private final ApiDataParser apiDataParser;
    private final FootballApiService footballApiService;
    private final CompetitionService competitionService;
    private final CompetitionRepository competitionRepository;
    private final GroupCompetitionRepository groupCompetitionRepository;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;

    // In-memory cache of full competition list from API
    private final List<CompetitionResponse> cachedCompetitions = new CopyOnWriteArrayList<>();

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

            List<CompetitionResponse> freshCache = new ArrayList<>();
            for (Map<String, Object> comp : apiCompetitions) {
                String country = (String) comp.get("country");
                String name = (String) comp.get("name");
                String code = (String) comp.get("code");
                if (country != null && name != null && code != null) {
                    Competition saved = competitionService.findOrCreate(country, name, code);
                    freshCache.add(CompetitionResponse.builder()
                            .id(saved.getId())
                            .country(saved.getCountry())
                            .name(saved.getName())
                            .code(saved.getCode())
                            .build());
                }
            }
            log.info("Upserted {} competition(s) from API", apiCompetitions.size());

            // Refresh in-memory cache with the full API list
            cachedCompetitions.clear();
            cachedCompetitions.addAll(freshCache);

            List<Competition> obsolete = competitionRepository.findByCodeNotIn(apiCodes);
            int removed = 0;
            for (Competition competition : obsolete) {
                boolean deleted = competitionService.deleteIfUnusedReturning(competition);
                if (deleted) {
                    removed++;
                    log.warn("Removed competition '{}' ({}) — no longer in API subscription",
                            competition.getName(), competition.getCode());
                } else {
                    log.info("Competition '{}' ({}) not in API but still referenced by tournaments — keeping",
                            competition.getName(), competition.getCode());
                }
            }

            if (removed > 0) {
                log.info("DB sync complete: removed {} obsolete competition(s)", removed);
            }
        } catch (Exception e) {
            log.error("Error during competitions DB sync", e);
        }
    }

    // Returns full list of all API competitions — used for the selection UI.
    // Falls back to DB if the in-memory cache is empty (e.g. API was down at startup).
    public List<CompetitionResponse> getCompetitionsList() {
        if (!cachedCompetitions.isEmpty()) {
            return List.copyOf(cachedCompetitions);
        }
        log.warn("In-memory competition cache is empty, falling back to DB");
        return competitionRepository.findAll().stream()
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
        log.warn("Scheduling retry for '{}' at {}", description, retryTime);
        taskScheduler.schedule(task, retryTime);
    }
}
