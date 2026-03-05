package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.dymohlo.sportPredictions.dto.response.SchedulerStatusResponse;
import ua.dymohlo.sportPredictions.entity.SchedulerStatus;
import ua.dymohlo.sportPredictions.enums.SchedulerRunStatus;
import ua.dymohlo.sportPredictions.repository.SchedulerStatusRepository;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerStatusService {

    private static final ZoneId KYIV_ZONE = ZoneId.of("Europe/Kiev");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Value("${scheduler.cron}")
    private String schedulerCron;

    private final SchedulerStatusRepository schedulerStatusRepository;

    public void setRunning() {
        saveStatus(SchedulerRunStatus.RUNNING, null);
    }

    public void setCompleted() {
        saveStatus(SchedulerRunStatus.COMPLETED, ZonedDateTime.now(KYIV_ZONE).format(ISO_FMT));
    }

    public void setIncomplete() {
        saveStatus(SchedulerRunStatus.INCOMPLETE, ZonedDateTime.now(KYIV_ZONE).format(ISO_FMT));
    }

    public SchedulerStatusResponse getStatus() {
        try {
            SchedulerStatus entity = schedulerStatusRepository.findById(1L)
                    .orElse(new SchedulerStatus(1L, SchedulerRunStatus.INCOMPLETE, null));
            return SchedulerStatusResponse.builder()
                    .status(entity.getStatus().name())
                    .lastRunAt(entity.getLastRunAt())
                    .nextRunAt(computeNextRunAt())
                    .build();
        } catch (Exception e) {
            log.error("Failed to read scheduler status from DB", e);
            return incompleteDefault();
        }
    }

    private void saveStatus(SchedulerRunStatus status, String lastRunAt) {
        try {
            SchedulerStatus entity = schedulerStatusRepository.findById(1L)
                    .orElse(new SchedulerStatus(1L, status, lastRunAt));
            entity.setStatus(status);
            if (lastRunAt != null) {
                entity.setLastRunAt(lastRunAt);
            }
            schedulerStatusRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to save scheduler status to DB", e);
        }
    }

    private String computeNextRunAt() {
        ZonedDateTime now = ZonedDateTime.now(KYIV_ZONE);
        LocalTime runTime = parseCronTime(schedulerCron);
        ZonedDateTime nextRun = now.toLocalDate().atTime(runTime).atZone(KYIV_ZONE);
        if (!now.isBefore(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }
        return nextRun.format(ISO_FMT);
    }

    private LocalTime parseCronTime(String cron) {
        try {
            String[] parts = cron.trim().split("\\s+");
            int second = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            int hour = Integer.parseInt(parts[2]);
            return LocalTime.of(hour, minute, second);
        } catch (Exception e) {
            log.warn("Failed to parse scheduler cron '{}', defaulting to 07:09:30", cron);
            return LocalTime.of(7, 9, 30);
        }
    }

    private SchedulerStatusResponse incompleteDefault() {
        return SchedulerStatusResponse.builder()
                .status(SchedulerRunStatus.INCOMPLETE.name())
                .lastRunAt(null)
                .nextRunAt(computeNextRunAt())
                .build();
    }
}