package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.entity.Prediction;
import ua.dymohlo.sportPredictions.entity.User;

import java.time.LocalDate;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Optional<Prediction> findByUserAndMatchDate(User user, LocalDate matchDate);

    @Transactional
    void deleteByMatchDateBefore(LocalDate date);
}