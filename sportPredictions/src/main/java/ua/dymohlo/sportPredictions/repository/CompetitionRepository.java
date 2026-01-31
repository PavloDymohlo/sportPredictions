package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.dymohlo.sportPredictions.entity.Competition;

import java.util.Optional;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    Optional<Competition> findByCountryAndName(String country, String name);

    boolean existsByCountryAndName(String country, String name);

    Optional<Competition> findByCode(String code);
}
