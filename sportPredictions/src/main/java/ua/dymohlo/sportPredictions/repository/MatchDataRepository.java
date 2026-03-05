package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.entity.MatchData;

import java.time.LocalDate;
import java.util.Optional;

public interface MatchDataRepository extends JpaRepository<MatchData, Long> {

    Optional<MatchData> findByMatchDate(LocalDate matchDate);

    boolean existsByMatchDate(LocalDate matchDate);

    @Transactional
    void deleteByMatchDateBefore(LocalDate date);
}