package ua.dymohlo.sportPredictions.entity;

import jakarta.persistence.*;
import lombok.*;
import ua.dymohlo.sportPredictions.enums.SchedulerRunStatus;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "scheduler_status")
public class SchedulerStatus {

    @Id
    private Long id = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SchedulerRunStatus status;

    @Column(name = "last_run_at")
    private String lastRunAt;
}