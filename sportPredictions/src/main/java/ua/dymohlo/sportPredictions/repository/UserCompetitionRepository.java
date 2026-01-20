package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserCompetition;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCompetitionRepository extends JpaRepository<UserCompetition, Long> {
    @Query("SELECT uc FROM UserCompetition uc JOIN FETCH uc.competition WHERE uc.user = :user")
    List<UserCompetition> findByUser(User user);
    List<UserCompetition> findByCompetition(Competition competition);
    Optional<UserCompetition> findByUserAndCompetition(User user, Competition competition);
    boolean existsByUserAndCompetition(User user, Competition competition);
    long countByCompetition(Competition competition);
    void deleteByUserAndCompetition(User user, Competition competition);
}