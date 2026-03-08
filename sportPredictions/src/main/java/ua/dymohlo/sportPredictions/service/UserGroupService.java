package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.dto.request.CreateGroupCompetitionsRequest;
import ua.dymohlo.sportPredictions.dto.request.CreateUserGroupRequest;
import ua.dymohlo.sportPredictions.dto.request.JoinGroupRequest;
import ua.dymohlo.sportPredictions.dto.request.ProcessJoinRequestRequest;
import ua.dymohlo.sportPredictions.dto.request.UpdateTournamentDatesRequest;
import ua.dymohlo.sportPredictions.dto.response.GroupJoinRequestResponse;
import ua.dymohlo.sportPredictions.dto.response.GroupTournamentResponse;
import ua.dymohlo.sportPredictions.dto.response.UserGroupResponse;
import ua.dymohlo.sportPredictions.entity.Competition;
import ua.dymohlo.sportPredictions.entity.GroupCompetition;
import ua.dymohlo.sportPredictions.entity.GroupJoinRequest;
import ua.dymohlo.sportPredictions.entity.GroupTournament;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.entity.UserGroup;
import ua.dymohlo.sportPredictions.enums.CompetitionStatus;
import ua.dymohlo.sportPredictions.enums.RequestStatus;
import ua.dymohlo.sportPredictions.repository.GroupCompetitionRepository;
import ua.dymohlo.sportPredictions.repository.GroupJoinRequestRepository;
import ua.dymohlo.sportPredictions.repository.GroupTournamentRepository;
import ua.dymohlo.sportPredictions.repository.GroupUserStatisticsRepository;
import ua.dymohlo.sportPredictions.repository.UserGroupRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserGroupService {

    private static final int MAX_GROUPS_PER_LEADER = 3;
    private static final int MAX_ACTIVE_TOURNAMENTS_PER_GROUP = 3;
    private static final String ACTION_ACCEPT = "ACCEPT";
    private static final String ACTION_REJECT = "REJECT";

    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final CompetitionService competitionService;
    private final GroupCompetitionRepository groupCompetitionRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final GroupTournamentRepository groupTournamentRepository;
    private final GroupUserStatisticsRepository groupUserStatisticsRepository;

    @Transactional
    public UserGroupResponse createUserGroup(CreateUserGroupRequest createUserGroupRequest, String leaderName) {
        log.info("Creating group: groupName='{}', leaderName='{}'",
                createUserGroupRequest.getUserGroupName(), leaderName);

        User user = userRepository.findByUserName(leaderName)
                .orElseThrow(() -> new IllegalArgumentException("User with username '"
                        + leaderName + "' not found!"));

        if (userGroupRepository.countByGroupLeader(user) >= MAX_GROUPS_PER_LEADER) {
            throw new IllegalArgumentException("You can create at most " + MAX_GROUPS_PER_LEADER + " groups");
        }

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

        return UserGroupResponse.builder()
                .userGroupName(group.getGroupName())
                .userGroupLeaderName(user.getUserName())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserGroupResponse> findAllUserGroup(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("User with username "
                        + username + " not found!"));

        return userGroupRepository.findAllGroupsForUser(user).stream()
                .map(g -> UserGroupResponse.builder()
                        .userGroupName(g.getGroupName())
                        .userGroupLeaderName(g.getGroupLeader().getUserName())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public UserGroupResponse findUserGroup(String userGroupName) {
        return userGroupRepository.findByGroupName(userGroupName)
                .map(g -> UserGroupResponse.builder()
                        .userGroupName(g.getGroupName())
                        .userGroupLeaderName(g.getGroupLeader().getUserName())
                        .build())
                .orElseThrow(() -> new IllegalArgumentException("Group with this name not found"));
    }

    @Transactional(readOnly = true)
    public boolean isUserMemberOfGroup(String groupName, String username) {
        return userGroupRepository.isUserMemberOfGroup(groupName, username);
    }

    @Transactional
    public void setGroupCompetitions(CreateGroupCompetitionsRequest request, String leaderName) {
        UserGroup group = userGroupRepository.findByGroupName(request.getGroupName())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        User leader = userRepository.findByUserName(leaderName)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateIsLeader(group, leader.getUserName());

        long activeFutureCount = groupTournamentRepository.countByUserGroupAndStatusIn(
                group, List.of(CompetitionStatus.ACTIVE, CompetitionStatus.NOT_STARTED));
        if (activeFutureCount >= MAX_ACTIVE_TOURNAMENTS_PER_GROUP) {
            throw new IllegalArgumentException("Group can have at most "
                    + MAX_ACTIVE_TOURNAMENTS_PER_GROUP + " active or upcoming tournaments");
        }

        GroupTournament tournament = GroupTournament.builder()
                .userGroup(group)
                .startDate(request.getStartDate())
                .finishDate(request.getEndDate())
                .status(resolveInitialStatus(request.getStartDate(), request.getEndDate()))
                .build();

        groupTournamentRepository.save(tournament);

        for (String compString : request.getCompetitions()) {
            String[] parts = compString.split(":");
            if (parts.length < 3) {
                throw new IllegalArgumentException(
                        "Invalid competition format (expected country:name:code): " + compString);
            }

            Competition comp = competitionService.findOrCreate(parts[0], parts[1], parts[2]);

            groupCompetitionRepository.save(GroupCompetition.builder()
                    .groupTournament(tournament)
                    .competition(comp)
                    .build());
        }

        log.info("Created new tournament for group '{}': {} to {}",
                group.getGroupName(), request.getStartDate(), request.getEndDate());
    }

    @Transactional(readOnly = true)
    public List<GroupTournamentResponse> getGroupTournaments(String groupName) {
        UserGroup group = userGroupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        return groupTournamentRepository.findByUserGroup(group).stream()
                .map(t -> GroupTournamentResponse.builder()
                        .id(t.getId())
                        .startDate(t.getStartDate())
                        .endDate(t.getFinishDate())
                        .status(t.getStatus().name())
                        .competitions(groupCompetitionRepository.findByGroupTournament(t).stream()
                                .map(gc -> gc.getCompetition().getCountry() + ":" + gc.getCompetition().getName())
                                .toList())
                        .winner(t.getWinner() != null ? t.getWinner().getUserName() : null)
                        .build())
                .toList();
    }

    @Transactional
    public void createJoinRequest(JoinGroupRequest request, String userName) {
        UserGroup group = userGroupRepository.findByGroupName(request.getGroupName())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (group.getUsers() != null && group.getUsers().contains(user)) {
            throw new IllegalArgumentException("You are already a member of this group");
        }

        if (groupJoinRequestRepository.existsByUserAndUserGroupAndStatus(user, group, RequestStatus.PENDING)) {
            throw new IllegalArgumentException("You already have a pending request for this group");
        }

        groupJoinRequestRepository.save(GroupJoinRequest.builder()
                .userGroup(group)
                .user(user)
                .status(RequestStatus.PENDING)
                .build());

        log.info("Join request created: user={}, group={}", user.getUserName(), group.getGroupName());
    }

    @Transactional(readOnly = true)
    public List<GroupJoinRequestResponse> getPendingRequests(String groupName) {
        UserGroup group = userGroupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        return groupJoinRequestRepository.findByUserGroupAndStatus(group, RequestStatus.PENDING).stream()
                .map(req -> GroupJoinRequestResponse.builder()
                        .userName(req.getUser().getUserName())
                        .status(req.getStatus().name())
                        .createdAt(req.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void processJoinRequest(ProcessJoinRequestRequest request, String leaderName) {
        UserGroup group = userGroupRepository.findByGroupName(request.getGroupName())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        User leader = userRepository.findByUserName(leaderName)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateIsLeader(group, leader.getUserName());

        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        GroupJoinRequest joinRequest = groupJoinRequestRepository.findByUserAndUserGroupAndStatus(
                user, group, RequestStatus.PENDING
        ).orElseThrow(() -> new IllegalArgumentException("Join request not found"));

        if (ACTION_ACCEPT.equalsIgnoreCase(request.getAction())) {
            if (group.getUsers() == null) {
                group.setUsers(new ArrayList<>());
            }
            group.getUsers().add(user);
            userGroupRepository.save(group);
            joinRequest.setStatus(RequestStatus.APPROVED);
            groupJoinRequestRepository.save(joinRequest);
            log.info("User {} accepted to group {}", user.getUserName(), group.getGroupName());

        } else if (ACTION_REJECT.equalsIgnoreCase(request.getAction())) {
            joinRequest.setStatus(RequestStatus.REJECTED);
            groupJoinRequestRepository.save(joinRequest);
            log.info("User {} rejected from group {}", user.getUserName(), group.getGroupName());

        } else {
            throw new IllegalArgumentException("Invalid action. Use ACCEPT or REJECT");
        }
    }

    @Transactional
    public void updateTournamentDates(UpdateTournamentDatesRequest request, String leaderName) {
        GroupTournament tournament = groupTournamentRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        UserGroup group = tournament.getUserGroup();

        User leader = userRepository.findByUserName(leaderName)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateIsLeader(group, leader.getUserName());

        if (tournament.getStatus() == CompetitionStatus.FINISHED) {
            throw new IllegalArgumentException("Cannot change dates of a finished tournament");
        }

        LocalDate today = LocalDate.now();

        if (request.getStartDate() != null) {
            if (tournament.getStatus() == CompetitionStatus.ACTIVE) {
                throw new IllegalArgumentException("Cannot change start date of an already active tournament");
            }
            if (request.getStartDate().isBefore(today)) {
                throw new IllegalArgumentException(
                        "Start date cannot be earlier than today (" + today + ")");
            }
            tournament.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            if (today.isAfter(tournament.getFinishDate())) {
                throw new IllegalArgumentException(
                        "End date cannot be changed — the last day has already been calculated");
            }
            tournament.setFinishDate(request.getEndDate());
        }

        if (tournament.getStartDate() != null && tournament.getFinishDate() != null
                && tournament.getStartDate().isAfter(tournament.getFinishDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        groupTournamentRepository.save(tournament);
        log.info("Tournament {} dates updated: {} to {}",
                tournament.getId(), tournament.getStartDate(), tournament.getFinishDate());
    }

    @Transactional
    public void deleteGroup(String groupName, String leaderName) {
        UserGroup group = userGroupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        validateIsLeader(group, leaderName);

        List<GroupTournament> tournaments = groupTournamentRepository.findByUserGroup(group);
        Set<Competition> competitionsToCheck = new HashSet<>();
        for (GroupTournament tournament : tournaments) {
            competitionsToCheck.addAll(collectAndDeleteTournamentData(tournament));
        }
        groupTournamentRepository.deleteAll(tournaments);
        groupJoinRequestRepository.deleteByUserGroup(group);
        userGroupRepository.delete(group);
        competitionsToCheck.forEach(competitionService::deleteIfUnused);

        log.info("🗑️ Group '{}' deleted by leader '{}'", groupName, leaderName);
    }

    @Transactional
    public void deleteTournament(Long tournamentId, String leaderName) {
        GroupTournament tournament = groupTournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        UserGroup group = tournament.getUserGroup();

        validateIsLeader(group, leaderName);

        Set<Competition> competitionsToCheck = collectAndDeleteTournamentData(tournament);

        groupTournamentRepository.delete(tournament);
        competitionsToCheck.forEach(competitionService::deleteIfUnused);

        log.info("🗑️ Tournament {} deleted from group '{}'", tournamentId, group.getGroupName());
    }

    @Transactional(readOnly = true)
    public List<String> getGroupMembers(String groupName) {
        UserGroup group = userGroupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        return group.getUsers().stream()
                .map(User::getUserName)
                .toList();
    }

    @Transactional
    public void deleteGroupInternal(UserGroup group) {
        List<GroupTournament> tournaments = groupTournamentRepository.findByUserGroup(group);
        Set<Competition> competitionsToCheck = new HashSet<>();
        for (GroupTournament tournament : tournaments) {
            competitionsToCheck.addAll(collectAndDeleteTournamentData(tournament));
        }
        groupTournamentRepository.deleteAll(tournaments);
        groupJoinRequestRepository.deleteByUserGroup(group);
        userGroupRepository.delete(group);
        competitionsToCheck.forEach(competitionService::deleteIfUnused);
        log.info("🗑️ Group '{}' deleted internally", group.getGroupName());
    }

    private Set<Competition> collectAndDeleteTournamentData(GroupTournament tournament) {
        Set<Competition> competitions = groupCompetitionRepository.findByGroupTournament(tournament).stream()
                .map(GroupCompetition::getCompetition)
                .collect(Collectors.toSet());
        groupUserStatisticsRepository.deleteByGroupTournament(tournament);
        groupCompetitionRepository.deleteByGroupTournament(tournament);
        return competitions;
    }

    private CompetitionStatus resolveInitialStatus(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return CompetitionStatus.NOT_STARTED;
        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate)) return CompetitionStatus.NOT_STARTED;
        if (today.isAfter(endDate)) return CompetitionStatus.FINISHED;
        return CompetitionStatus.ACTIVE;
    }

    private void validateIsLeader(UserGroup group, String leaderName) {
        if (!group.getGroupLeader().getUserName().equals(leaderName)) {
            throw new IllegalArgumentException("Only group leader can perform this action");
        }
    }
}
