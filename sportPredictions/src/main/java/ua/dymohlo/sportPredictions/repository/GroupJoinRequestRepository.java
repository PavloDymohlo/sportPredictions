package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.entity.GroupJoinRequest;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserGroup;
import ua.dymohlo.sportPredictions.enums.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {
    List<GroupJoinRequest> findByUserGroupAndStatus(
            UserGroup userGroup,
            RequestStatus status
    );

    boolean existsByUserAndUserGroupAndStatus(
            User user,
            UserGroup userGroup,
            RequestStatus status
    );

    Optional<GroupJoinRequest> findByUserAndUserGroupAndStatus(
            User user,
            UserGroup userGroup,
            RequestStatus status
    );

    @Transactional
    void deleteByUserGroup(UserGroup userGroup);
}