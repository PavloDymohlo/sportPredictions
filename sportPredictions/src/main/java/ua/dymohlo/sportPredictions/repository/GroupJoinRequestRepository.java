package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.dymohlo.sportPredictions.entity.GroupJoinRequest;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserGroup;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {
    List<GroupJoinRequest> findByUserGroupAndStatus(
            UserGroup userGroup,
            GroupJoinRequest.RequestStatus status
    );

    boolean existsByUserAndUserGroupAndStatus(
            User user,
            UserGroup userGroup,
            GroupJoinRequest.RequestStatus status
    );

    Optional<GroupJoinRequest> findByIdAndUserGroup(
            Long id,
            UserGroup userGroup
    );

    Optional<GroupJoinRequest> findByUserAndUserGroupAndStatus(
            User user,
            UserGroup userGroup,
            GroupJoinRequest.RequestStatus status
    );

    void deleteByUserAndUserGroup(User user, UserGroup userGroup);
}