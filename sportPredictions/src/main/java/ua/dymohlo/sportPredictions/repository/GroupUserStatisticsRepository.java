package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.entity.GroupTournament;
import ua.dymohlo.sportPredictions.entity.GroupUserStatistics;
import ua.dymohlo.sportPredictions.entity.User;

import java.util.List;
import java.util.Optional;

public interface GroupUserStatisticsRepository extends JpaRepository<GroupUserStatistics, Long> {

    Optional<GroupUserStatistics> findByGroupTournamentAndUser(GroupTournament groupTournament, User user);

    @Query("""
            SELECT gus FROM GroupUserStatistics gus
            WHERE gus.groupTournament = :groupTournament
            ORDER BY gus.correctPredictions DESC, gus.accuracyPercent DESC
            """)
    List<GroupUserStatistics> findByGroupTournamentOrderedByRanking(@Param("groupTournament") GroupTournament groupTournament);

    @Transactional
    void deleteByGroupTournament(GroupTournament groupTournament);
}
