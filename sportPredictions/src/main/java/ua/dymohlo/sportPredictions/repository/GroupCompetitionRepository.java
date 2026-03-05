package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.entity.GroupCompetition;
import ua.dymohlo.sportPredictions.entity.GroupTournament;

import java.util.List;

public interface GroupCompetitionRepository extends JpaRepository<GroupCompetition, Long> {

    @Query("SELECT gc FROM GroupCompetition gc JOIN FETCH gc.competition WHERE gc.groupTournament = :tournament")
    List<GroupCompetition> findByGroupTournament(@Param("tournament") GroupTournament tournament);

    @Transactional
    void deleteByGroupTournament(GroupTournament groupTournament);

    boolean existsByCompetition(Competition competition);

    @Transactional
    void deleteByCompetition(Competition competition);
}
