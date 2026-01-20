package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserGroup;

import java.util.List;
import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
    Optional<UserGroup> findByGroupName(String userGroupName);

    @Query("SELECT g FROM UserGroup g " +
            "LEFT JOIN g.users u " +
            "WHERE g.groupLeader = :user OR u = :user")
    List<UserGroup> findAllGroupsForUser(@Param("user") User user);

}

