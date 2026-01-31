package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.entity.GroupCompetition;
import ua.dymohlo.sportPredictions.entity.UserGroup;

import java.util.List;

public interface GroupCompetitionRepository extends JpaRepository<GroupCompetition, Long> {
    List<GroupCompetition> findByUserGroup(UserGroup userGroup);

    boolean existsByUserGroupAndCompetition(UserGroup userGroup, Competition competition);

    void deleteByUserGroupAndCompetition(UserGroup userGroup, Competition competition);

    void deleteByUserGroup(UserGroup group);
}
