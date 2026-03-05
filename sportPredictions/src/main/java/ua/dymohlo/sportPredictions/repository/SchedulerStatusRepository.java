package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.dymohlo.sportPredictions.entity.SchedulerStatus;

public interface SchedulerStatusRepository extends JpaRepository<SchedulerStatus, Long> {
}