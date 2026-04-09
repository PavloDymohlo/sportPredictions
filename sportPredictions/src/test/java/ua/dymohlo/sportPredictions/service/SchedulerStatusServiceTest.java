package ua.dymohlo.sportPredictions.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ua.dymohlo.sportPredictions.dto.response.SchedulerStatusResponse;
import ua.dymohlo.sportPredictions.entity.SchedulerStatus;
import ua.dymohlo.sportPredictions.enums.SchedulerRunStatus;
import ua.dymohlo.sportPredictions.repository.SchedulerStatusRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerStatusServiceTest {

    @Mock
    private SchedulerStatusRepository schedulerStatusRepository;

    @InjectMocks
    private SchedulerStatusService schedulerStatusService;

    @BeforeEach
    void setUp() {
        // 07:09:30 = "30 9 7 * * *" in Spring cron, or as stored "30 9 7"
        ReflectionTestUtils.setField(schedulerStatusService, "schedulerCron", "30 9 7 * * *");
    }

    // ─── setRunning ───────────────────────────────────────────────────────────

    @Test
    void setRunning_savesRunningStatus() {
        when(schedulerStatusRepository.findById(1L)).thenReturn(Optional.empty());
        when(schedulerStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        schedulerStatusService.setRunning();

        ArgumentCaptor<SchedulerStatus> captor = ArgumentCaptor.forClass(SchedulerStatus.class);
        verify(schedulerStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SchedulerRunStatus.RUNNING);
    }

    // ─── setCompleted ─────────────────────────────────────────────────────────

    @Test
    void setCompleted_savesCompletedStatusWithTimestamp() {
        when(schedulerStatusRepository.findById(1L)).thenReturn(Optional.empty());
        when(schedulerStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        schedulerStatusService.setCompleted();

        ArgumentCaptor<SchedulerStatus> captor = ArgumentCaptor.forClass(SchedulerStatus.class);
        verify(schedulerStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SchedulerRunStatus.COMPLETED);
        assertThat(captor.getValue().getLastRunAt()).isNotBlank();
    }

    // ─── setIncomplete ────────────────────────────────────────────────────────

    @Test
    void setIncomplete_savesIncompleteStatusWithTimestamp() {
        when(schedulerStatusRepository.findById(1L)).thenReturn(Optional.empty());
        when(schedulerStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        schedulerStatusService.setIncomplete();

        ArgumentCaptor<SchedulerStatus> captor = ArgumentCaptor.forClass(SchedulerStatus.class);
        verify(schedulerStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SchedulerRunStatus.INCOMPLETE);
        assertThat(captor.getValue().getLastRunAt()).isNotBlank();
    }

    // ─── getStatus ────────────────────────────────────────────────────────────

    @Test
    void getStatus_existingEntity_returnsMappedResponse() {
        SchedulerStatus entity = new SchedulerStatus(1L, SchedulerRunStatus.COMPLETED, "2025-03-10T07:09:30+02:00");
        when(schedulerStatusRepository.findById(1L)).thenReturn(Optional.of(entity));

        SchedulerStatusResponse response = schedulerStatusService.getStatus();

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getLastRunAt()).isEqualTo("2025-03-10T07:09:30+02:00");
        assertThat(response.getNextRunAt()).isNotBlank();
    }

    @Test
    void getStatus_noEntity_returnsIncompleteDefault() {
        when(schedulerStatusRepository.findById(1L)).thenReturn(Optional.empty());

        SchedulerStatusResponse response = schedulerStatusService.getStatus();

        assertThat(response.getStatus()).isEqualTo("INCOMPLETE");
        assertThat(response.getNextRunAt()).isNotBlank();
    }

    @Test
    void getStatus_repositoryThrows_returnsIncompleteDefault() {
        when(schedulerStatusRepository.findById(1L)).thenThrow(new RuntimeException("DB error"));

        SchedulerStatusResponse response = schedulerStatusService.getStatus();

        assertThat(response.getStatus()).isEqualTo("INCOMPLETE");
    }

    // ─── computeNextRunAt (via getStatus) ─────────────────────────────────────

    @Test
    void getStatus_nextRunAt_isNotNull() {
        when(schedulerStatusRepository.findById(1L))
                .thenReturn(Optional.of(new SchedulerStatus(1L, SchedulerRunStatus.COMPLETED, null)));

        SchedulerStatusResponse response = schedulerStatusService.getStatus();

        assertThat(response.getNextRunAt()).isNotNull();
    }

    // ─── updateExistingEntity ─────────────────────────────────────────────────

    @Test
    void setRunning_existingEntity_updatesStatusInPlace() {
        SchedulerStatus existing = new SchedulerStatus(1L, SchedulerRunStatus.COMPLETED, "prev");
        when(schedulerStatusRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(schedulerStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        schedulerStatusService.setRunning();

        assertThat(existing.getStatus()).isEqualTo(SchedulerRunStatus.RUNNING);
        // lastRunAt should remain unchanged when setting RUNNING (null passed)
        assertThat(existing.getLastRunAt()).isEqualTo("prev");
    }
}
