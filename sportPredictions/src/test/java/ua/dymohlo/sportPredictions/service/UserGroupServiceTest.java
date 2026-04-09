package ua.dymohlo.sportPredictions.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.dymohlo.sportPredictions.dto.request.*;
import ua.dymohlo.sportPredictions.dto.response.GroupTournamentResponse;
import ua.dymohlo.sportPredictions.dto.response.UserGroupResponse;
import ua.dymohlo.sportPredictions.entity.*;
import ua.dymohlo.sportPredictions.enums.CompetitionStatus;
import ua.dymohlo.sportPredictions.enums.RequestStatus;
import ua.dymohlo.sportPredictions.repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserGroupServiceTest {

    @Mock
    private UserGroupRepository userGroupRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CompetitionService competitionService;
    @Mock
    private GroupCompetitionRepository groupCompetitionRepository;
    @Mock
    private GroupJoinRequestRepository groupJoinRequestRepository;
    @Mock
    private GroupTournamentRepository groupTournamentRepository;
    @Mock
    private GroupUserStatisticsRepository groupUserStatisticsRepository;

    @InjectMocks
    private UserGroupService userGroupService;

    private User makeUser(Long id, String name) {
        return User.builder().id(id).userName(name).build();
    }

    private UserGroup makeGroup(Long id, String name, User leader) {
        return UserGroup.builder()
                .id(id).groupName(name).groupLeader(leader)
                .users(new ArrayList<>(List.of(leader)))
                .build();
    }

    // ─── createUserGroup ──────────────────────────────────────────────────────

    @Test
    void createUserGroup_success() {
        User leader = makeUser(1L, "alice");
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));
        when(userGroupRepository.countByGroupLeader(leader)).thenReturn(0L);
        when(userGroupRepository.findByGroupName("MyGroup")).thenReturn(Optional.empty());
        when(userGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserGroupResponse response = userGroupService.createUserGroup(
                new CreateUserGroupRequest("MyGroup"), "alice");

        assertThat(response.getUserGroupName()).isEqualTo("MyGroup");
        assertThat(response.getUserGroupLeaderName()).isEqualTo("alice");
    }

    @Test
    void createUserGroup_userNotFound_throws() {
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userGroupService.createUserGroup(new CreateUserGroupRequest("G"), "ghost"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createUserGroup_leaderLimitExceeded_throws() {
        User leader = makeUser(1L, "alice");
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));
        when(userGroupRepository.countByGroupLeader(leader)).thenReturn(3L);

        assertThatThrownBy(() ->
                userGroupService.createUserGroup(new CreateUserGroupRequest("G"), "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 3");
    }

    @Test
    void createUserGroup_nameAlreadyExists_throws() {
        User leader = makeUser(1L, "alice");
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));
        when(userGroupRepository.countByGroupLeader(leader)).thenReturn(0L);
        when(userGroupRepository.findByGroupName("Taken")).thenReturn(Optional.of(makeGroup(1L, "Taken", leader)));

        assertThatThrownBy(() ->
                userGroupService.createUserGroup(new CreateUserGroupRequest("Taken"), "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    // ─── findAllUserGroup ─────────────────────────────────────────────────────

    @Test
    void findAllUserGroup_returnsGroupsForUser() {
        User user = makeUser(1L, "bob");
        UserGroup g1 = makeGroup(1L, "G1", user);
        UserGroup g2 = makeGroup(2L, "G2", user);
        when(userRepository.findByUserName("bob")).thenReturn(Optional.of(user));
        when(userGroupRepository.findAllGroupsForUser(user)).thenReturn(List.of(g1, g2));

        var result = userGroupService.findAllUserGroup("bob");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserGroupResponse::getUserGroupName)
                .containsExactly("G1", "G2");
    }

    @Test
    void findAllUserGroup_userNotFound_throws() {
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userGroupService.findAllUserGroup("ghost"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── findUserGroup ────────────────────────────────────────────────────────

    @Test
    void findUserGroup_found_returnsResponse() {
        User leader = makeUser(1L, "alice");
        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(makeGroup(1L, "G1", leader)));

        UserGroupResponse resp = userGroupService.findUserGroup("G1");

        assertThat(resp.getUserGroupName()).isEqualTo("G1");
        assertThat(resp.getUserGroupLeaderName()).isEqualTo("alice");
    }

    @Test
    void findUserGroup_notFound_throws() {
        when(userGroupRepository.findByGroupName("Missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userGroupService.findUserGroup("Missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── setGroupCompetitions ─────────────────────────────────────────────────

    @Test
    void setGroupCompetitions_success_createsNewTournament() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));
        when(groupTournamentRepository.countByUserGroupAndStatusIn(any(), any())).thenReturn(0L);
        when(groupTournamentRepository.save(any())).thenAnswer(inv -> {
            GroupTournament t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        Competition comp = Competition.builder().id(1L).country("England").name("PL").code("PL").build();
        when(competitionService.findOrCreate("England", "Premier League", "PL")).thenReturn(comp);
        when(groupCompetitionRepository.save(any())).thenReturn(null);

        CreateGroupCompetitionsRequest req = CreateGroupCompetitionsRequest.builder()
                .groupName("G1")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .competitions(List.of("England:Premier League:PL"))
                .build();

        userGroupService.setGroupCompetitions(req, "alice");

        verify(groupTournamentRepository).save(any(GroupTournament.class));
        verify(groupCompetitionRepository).save(any(GroupCompetition.class));
    }

    @Test
    void setGroupCompetitions_notLeader_throws() {
        User leader = makeUser(1L, "alice");
        User other = makeUser(2L, "bob");
        UserGroup group = makeGroup(1L, "G1", leader);

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(userRepository.findByUserName("bob")).thenReturn(Optional.of(other));

        CreateGroupCompetitionsRequest req = CreateGroupCompetitionsRequest.builder()
                .groupName("G1").startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(10))
                .competitions(List.of("A:B:C")).build();

        assertThatThrownBy(() -> userGroupService.setGroupCompetitions(req, "bob"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leader");
    }

    @Test
    void setGroupCompetitions_tournamentLimitReached_throws() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));
        when(groupTournamentRepository.countByUserGroupAndStatusIn(any(), any())).thenReturn(3L);

        CreateGroupCompetitionsRequest req = CreateGroupCompetitionsRequest.builder()
                .groupName("G1").startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(10))
                .competitions(List.of("A:B:C")).build();

        assertThatThrownBy(() -> userGroupService.setGroupCompetitions(req, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 3");
    }

    @Test
    void setGroupCompetitions_invalidCompetitionFormat_throws() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));
        when(groupTournamentRepository.countByUserGroupAndStatusIn(any(), any())).thenReturn(0L);
        when(groupTournamentRepository.save(any())).thenAnswer(inv -> {
            GroupTournament t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        CreateGroupCompetitionsRequest req = CreateGroupCompetitionsRequest.builder()
                .groupName("G1").startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(10))
                .competitions(List.of("BadFormat")).build();

        assertThatThrownBy(() -> userGroupService.setGroupCompetitions(req, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid competition format");
    }

    // ─── createJoinRequest ────────────────────────────────────────────────────

    @Test
    void createJoinRequest_success_savesRequest() {
        User leader = makeUser(1L, "alice");
        User requester = makeUser(2L, "bob");
        UserGroup group = makeGroup(1L, "G1", leader);
        group.setUsers(new ArrayList<>(List.of(leader)));

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(userRepository.findByUserName("bob")).thenReturn(Optional.of(requester));
        when(groupJoinRequestRepository.existsByUserAndUserGroupAndStatus(requester, group, RequestStatus.PENDING))
                .thenReturn(false);
        when(groupJoinRequestRepository.save(any())).thenReturn(null);

        userGroupService.createJoinRequest(new JoinGroupRequest("G1"), "bob");

        verify(groupJoinRequestRepository).save(any(GroupJoinRequest.class));
    }

    @Test
    void createJoinRequest_alreadyMember_throws() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        group.setUsers(new ArrayList<>(List.of(leader)));

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));

        assertThatThrownBy(() -> userGroupService.createJoinRequest(new JoinGroupRequest("G1"), "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void createJoinRequest_pendingRequestExists_throws() {
        User leader = makeUser(1L, "alice");
        User requester = makeUser(2L, "bob");
        UserGroup group = makeGroup(1L, "G1", leader);
        group.setUsers(new ArrayList<>(List.of(leader)));

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(userRepository.findByUserName("bob")).thenReturn(Optional.of(requester));
        when(groupJoinRequestRepository.existsByUserAndUserGroupAndStatus(requester, group, RequestStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> userGroupService.createJoinRequest(new JoinGroupRequest("G1"), "bob"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pending request");
    }

    // ─── processJoinRequest ───────────────────────────────────────────────────

    @Test
    void processJoinRequest_accept_addsUserToGroup() {
        User leader = makeUser(1L, "alice");
        User requester = makeUser(2L, "bob");
        UserGroup group = makeGroup(1L, "G1", leader);
        group.setUsers(new ArrayList<>(List.of(leader)));
        GroupJoinRequest joinRequest = GroupJoinRequest.builder()
                .id(1L).user(requester).userGroup(group).status(RequestStatus.PENDING).build();

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));
        when(userRepository.findByUserName("bob")).thenReturn(Optional.of(requester));
        when(groupJoinRequestRepository.findByUserAndUserGroupAndStatus(requester, group, RequestStatus.PENDING))
                .thenReturn(Optional.of(joinRequest));
        when(userGroupRepository.save(any())).thenReturn(group);
        when(groupJoinRequestRepository.save(any())).thenReturn(joinRequest);

        ProcessJoinRequestRequest req = new ProcessJoinRequestRequest("bob", "G1", "ACCEPT");
        userGroupService.processJoinRequest(req, "alice");

        assertThat(group.getUsers()).contains(requester);
        assertThat(joinRequest.getStatus()).isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    void processJoinRequest_reject_setsRejectedStatus() {
        User leader = makeUser(1L, "alice");
        User requester = makeUser(2L, "bob");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupJoinRequest joinRequest = GroupJoinRequest.builder()
                .id(1L).user(requester).userGroup(group).status(RequestStatus.PENDING).build();

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));
        when(userRepository.findByUserName("bob")).thenReturn(Optional.of(requester));
        when(groupJoinRequestRepository.findByUserAndUserGroupAndStatus(requester, group, RequestStatus.PENDING))
                .thenReturn(Optional.of(joinRequest));
        when(groupJoinRequestRepository.save(any())).thenReturn(joinRequest);

        ProcessJoinRequestRequest req = new ProcessJoinRequestRequest("bob", "G1", "REJECT");
        userGroupService.processJoinRequest(req, "alice");

        assertThat(joinRequest.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void processJoinRequest_invalidAction_throws() {
        User leader = makeUser(1L, "alice");
        User requester = makeUser(2L, "bob");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupJoinRequest joinRequest = GroupJoinRequest.builder()
                .id(1L).user(requester).userGroup(group).status(RequestStatus.PENDING).build();

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));
        when(userRepository.findByUserName("bob")).thenReturn(Optional.of(requester));
        when(groupJoinRequestRepository.findByUserAndUserGroupAndStatus(requester, group, RequestStatus.PENDING))
                .thenReturn(Optional.of(joinRequest));

        ProcessJoinRequestRequest req = new ProcessJoinRequestRequest("bob", "G1", "MAYBE");
        assertThatThrownBy(() -> userGroupService.processJoinRequest(req, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid action");
    }

    // ─── updateTournamentDates ────────────────────────────────────────────────

    @Test
    void updateTournamentDates_notStarted_updatesBothDates() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament tournament = GroupTournament.builder()
                .id(1L).userGroup(group)
                .startDate(LocalDate.now().plusDays(5))
                .finishDate(LocalDate.now().plusDays(30))
                .status(CompetitionStatus.NOT_STARTED)
                .build();

        when(groupTournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));
        when(groupTournamentRepository.save(any())).thenReturn(tournament);

        UpdateTournamentDatesRequest req = UpdateTournamentDatesRequest.builder()
                .tournamentId(1L)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(40))
                .build();

        userGroupService.updateTournamentDates(req, "alice");

        assertThat(tournament.getStartDate()).isEqualTo(LocalDate.now().plusDays(10));
        assertThat(tournament.getFinishDate()).isEqualTo(LocalDate.now().plusDays(40));
    }

    @Test
    void updateTournamentDates_finishedTournament_throws() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament tournament = GroupTournament.builder()
                .id(1L).userGroup(group)
                .startDate(LocalDate.now().minusDays(10))
                .finishDate(LocalDate.now().minusDays(1))
                .status(CompetitionStatus.FINISHED)
                .build();

        when(groupTournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));

        UpdateTournamentDatesRequest req = UpdateTournamentDatesRequest.builder()
                .tournamentId(1L).endDate(LocalDate.now().plusDays(5)).build();

        assertThatThrownBy(() -> userGroupService.updateTournamentDates(req, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finished tournament");
    }

    @Test
    void updateTournamentDates_activeTournamentChangeStartDate_throws() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament tournament = GroupTournament.builder()
                .id(1L).userGroup(group)
                .startDate(LocalDate.now().minusDays(5))
                .finishDate(LocalDate.now().plusDays(25))
                .status(CompetitionStatus.ACTIVE)
                .build();

        when(groupTournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(userRepository.findByUserName("alice")).thenReturn(Optional.of(leader));

        UpdateTournamentDatesRequest req = UpdateTournamentDatesRequest.builder()
                .tournamentId(1L).startDate(LocalDate.now().plusDays(3)).build();

        assertThatThrownBy(() -> userGroupService.updateTournamentDates(req, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("active tournament");
    }

    // ─── getGroupMembers ──────────────────────────────────────────────────────

    @Test
    void getGroupMembers_returnsAllMemberNames() {
        User leader = makeUser(1L, "alice");
        User member = makeUser(2L, "bob");
        UserGroup group = makeGroup(1L, "G1", leader);
        group.setUsers(new ArrayList<>(List.of(leader, member)));

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));

        List<String> members = userGroupService.getGroupMembers("G1");

        assertThat(members).containsExactlyInAnyOrder("alice", "bob");
    }

    // ─── getGroupTournaments ──────────────────────────────────────────────────

    @Test
    void getGroupTournaments_returnsMappedResponses() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament tournament = GroupTournament.builder()
                .id(1L).userGroup(group)
                .startDate(LocalDate.now().minusDays(5))
                .finishDate(LocalDate.now().plusDays(25))
                .status(CompetitionStatus.ACTIVE)
                .build();

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(groupTournamentRepository.findByUserGroup(group)).thenReturn(List.of(tournament));
        when(groupCompetitionRepository.findByGroupTournament(tournament)).thenReturn(List.of());

        List<GroupTournamentResponse> result = userGroupService.getGroupTournaments("G1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getStatus()).isEqualTo("ACTIVE");
    }

    // ─── deleteGroup ──────────────────────────────────────────────────────────

    @Test
    void deleteGroup_leader_deletesGroupAndTournaments() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);
        GroupTournament t = GroupTournament.builder().id(1L).userGroup(group).build();

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));
        when(groupTournamentRepository.findByUserGroup(group)).thenReturn(List.of(t));
        when(groupCompetitionRepository.findByGroupTournament(t)).thenReturn(List.of());
        doNothing().when(groupUserStatisticsRepository).deleteByGroupTournament(t);
        doNothing().when(groupCompetitionRepository).deleteByGroupTournament(t);
        doNothing().when(groupTournamentRepository).deleteAll(any());
        doNothing().when(groupJoinRequestRepository).deleteByUserGroup(group);
        doNothing().when(userGroupRepository).delete(group);

        userGroupService.deleteGroup("G1", "alice");

        verify(userGroupRepository).delete(group);
    }

    @Test
    void deleteGroup_notLeader_throws() {
        User leader = makeUser(1L, "alice");
        UserGroup group = makeGroup(1L, "G1", leader);

        when(userGroupRepository.findByGroupName("G1")).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> userGroupService.deleteGroup("G1", "notAlice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("leader");
    }
}
