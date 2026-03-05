package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.dymohlo.sportPredictions.entity.GroupTournament;
import ua.dymohlo.sportPredictions.entity.UserGroup;
import ua.dymohlo.sportPredictions.enums.CompetitionStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupTournamentRepository extends JpaRepository<GroupTournament, Long> {

    List<GroupTournament> findByUserGroup(UserGroup userGroup);

    List<GroupTournament> findByStatus(CompetitionStatus status);

    @Query("SELECT t FROM GroupTournament t WHERE t.userGroup = :userGroup AND t.startDate <= :date AND t.finishDate >= :date")
    Optional<GroupTournament> findByUserGroupAndDateInRange(@Param("userGroup") UserGroup userGroup,
                                                            @Param("date") LocalDate date);

    long countByUserGroupAndStatusIn(UserGroup userGroup, Collection<CompetitionStatus> statuses);

    @Query("SELECT t FROM GroupTournament t WHERE t.status = 'FINISHED' AND t.finishDate <= :cutoffDate")
    List<GroupTournament> findFinishedTournamentsOlderThan(@Param("cutoffDate") LocalDate cutoffDate);
}
