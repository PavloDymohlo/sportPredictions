package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.dto.request.CreateGroupCompetitionsRequest;
import ua.dymohlo.sportPredictions.dto.request.CreateUserGroupRequest;
import ua.dymohlo.sportPredictions.dto.request.JoinGroupRequest;
import ua.dymohlo.sportPredictions.dto.request.ProcessJoinRequestRequest;
import ua.dymohlo.sportPredictions.dto.response.GroupCompetitionsResponse;
import ua.dymohlo.sportPredictions.dto.response.GroupJoinRequestResponse;
import ua.dymohlo.sportPredictions.entity.*;
import ua.dymohlo.sportPredictions.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserGroupService {
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final CompetitionRepository competitionRepository;
    private final GroupCompetitionRepository groupCompetitionRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;

    public CreateUserGroupRequest createUserGroup(CreateUserGroupRequest createUserGroupRequest) {
        log.info("Creating group: groupName='{}', leaderName='{}'",
                createUserGroupRequest.getUserGroupName(),
                createUserGroupRequest.getUserGroupLeaderName());

        User user = userRepository.findByUserName(createUserGroupRequest.getUserGroupLeaderName())
                .orElseThrow(() -> new IllegalArgumentException("User with username '"
                        + createUserGroupRequest.getUserGroupLeaderName() + "' not found!"));

        userGroupRepository.findByGroupName(createUserGroupRequest.getUserGroupName())
                .ifPresent(group -> {
                    throw new IllegalArgumentException("Group with name '"
                            + createUserGroupRequest.getUserGroupName() + "' already exists.");
                });

        UserGroup group = UserGroup.builder()
                .groupName(createUserGroupRequest.getUserGroupName())
                .groupLeader(user)
                .users(new ArrayList<>(List.of(user)))
                .build();

        userGroupRepository.save(group);

        log.info("Group '{}' created successfully with leader '{}'",
                group.getGroupName(), user.getUserName());

        return createUserGroupRequest;
    }

    /**
     * all groups for concrete user
     */
    public List<CreateUserGroupRequest> findAllUserGroup(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("User with username "
                        + username + " not found!"));

        List<UserGroup> groups = userGroupRepository.findAllGroupsForUser(user);

        return groups.stream()
                .map(g -> CreateUserGroupRequest.builder()
                        .userGroupName(g.getGroupName())
                        .userGroupLeaderName(g.getGroupLeader().getUserName())
                        .build())
                .collect(Collectors.toList());
    }

    public CreateUserGroupRequest findUserGroup(String userGroupName) {
        return userGroupRepository.findByGroupName(userGroupName)
                .map(g -> CreateUserGroupRequest.builder()
                        .userGroupName(g.getGroupName())
                        .userGroupLeaderName(g.getGroupLeader().getUserName()))
                .orElseThrow(() -> new IllegalArgumentException("Group with this name not found")).build();
    }

    @Transactional
    public void setGroupCompetitions(CreateGroupCompetitionsRequest request) {
        UserGroup group = userGroupRepository.findByGroupName(request.getGroupName())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        User leader = userRepository.findByUserName(request.getLeaderName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!group.getGroupLeader().getId().equals(leader.getId())) {
            throw new IllegalArgumentException("Only group leader can set competitions");
        }

        group.setStartDate(request.getStartDate());
        group.setFinishDate(request.getEndDate());
        userGroupRepository.save(group);

        groupCompetitionRepository.deleteByUserGroup(group);

        for (String compString : request.getCompetitions()) {
            String[] parts = compString.split(":");
            Competition comp = competitionRepository
                    .findByCountryAndName(parts[0], parts[1])
                    .orElseThrow(() -> new IllegalArgumentException("Competition not found"));

            GroupCompetition gc = GroupCompetition.builder()
                    .userGroup(group)
                    .competition(comp)
                    .build();

            groupCompetitionRepository.save(gc);
        }
    }

    public GroupCompetitionsResponse getGroupCompetitions(String groupName) {
        UserGroup group = userGroupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        List<String> competitions = group.getCompetitions().stream()
                .map(comp -> comp.getCountry() + ":" + comp.getName())
                .collect(Collectors.toList());

        return GroupCompetitionsResponse.builder()
                .startDate(group.getStartDate())
                .endDate(group.getFinishDate())
                .competitions(competitions)
                .build();
    }

    @Transactional
    public void createJoinRequest(JoinGroupRequest request) {
        UserGroup group = userGroupRepository.findByGroupName(request.getGroupName())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (group.getUsers() != null && group.getUsers().contains(user)) {
            throw new IllegalArgumentException("You are already a member of this group");
        }

        boolean hasPendingRequest = groupJoinRequestRepository.existsByUserAndUserGroupAndStatus(
                user, group, GroupJoinRequest.RequestStatus.PENDING
        );

        if (hasPendingRequest) {
            throw new IllegalArgumentException("You already have a pending request for this group");
        }

        GroupJoinRequest joinRequest = GroupJoinRequest.builder()
                .userGroup(group)
                .user(user)
                .status(GroupJoinRequest.RequestStatus.PENDING)
                .build();

        groupJoinRequestRepository.save(joinRequest);
        log.info("Join request created: user={}, group={}", user.getUserName(), group.getGroupName());
    }

    public List<GroupJoinRequestResponse> getPendingRequests(String groupName) {
        UserGroup group = userGroupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        List<GroupJoinRequest> requests = groupJoinRequestRepository.findByUserGroupAndStatus(
                group, GroupJoinRequest.RequestStatus.PENDING
        );

        return requests.stream()
                .map(req -> GroupJoinRequestResponse.builder()
                        .userName(req.getUser().getUserName())
                        .status(req.getStatus().name())
                        .createdAt(req.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void processJoinRequest(ProcessJoinRequestRequest request) {
        UserGroup group = userGroupRepository.findByGroupName(request.getGroupName())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        User leader = userRepository.findByUserName(request.getLeaderName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!group.getGroupLeader().getId().equals(leader.getId())) {
            throw new IllegalArgumentException("Only group leader can process requests");
        }

        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        GroupJoinRequest joinRequest = groupJoinRequestRepository.findByUserAndUserGroupAndStatus(
                user, group, GroupJoinRequest.RequestStatus.PENDING
        ).orElseThrow(() -> new IllegalArgumentException("Join request not found"));

        if ("ACCEPT".equalsIgnoreCase(request.getAction())) {
            if (group.getUsers() == null) {
                group.setUsers(new ArrayList<>());
            }
            group.getUsers().add(user);
            userGroupRepository.save(group);

            joinRequest.setStatus(GroupJoinRequest.RequestStatus.APPROVED);
            groupJoinRequestRepository.save(joinRequest);

            log.info("User {} accepted to group {}", user.getUserName(), group.getGroupName());

        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {

            joinRequest.setStatus(GroupJoinRequest.RequestStatus.REJECTED);
            groupJoinRequestRepository.save(joinRequest);

            log.info("User {} rejected from group {}", user.getUserName(), group.getGroupName());

        } else {
            throw new IllegalArgumentException("Invalid action. Use ACCEPT or REJECT");
        }
    }

    public List<String> getGroupMembers(String groupName) {
        UserGroup group = userGroupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        return group.getUsers().stream()
                .map(User::getUserName)
                .collect(Collectors.toList());
    }
}
