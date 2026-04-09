package ua.dymohlo.sportPredictions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ua.dymohlo.sportPredictions.dto.request.*;
import ua.dymohlo.sportPredictions.dto.response.*;
import ua.dymohlo.sportPredictions.secutity.JwtUtils;
import ua.dymohlo.sportPredictions.service.GroupMatchesService;
import ua.dymohlo.sportPredictions.service.GroupStatisticsService;
import ua.dymohlo.sportPredictions.service.UserGroupService;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests for group management endpoints.
 * @WithMockUser sets the SecurityContext so Authentication is injected into controllers.
 * .with(csrf()) satisfies CSRF validation for mutation requests.
 */
@WebMvcTest(UserGroupController.class)
class UserGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserGroupService userGroupService;

    @MockitoBean
    private GroupStatisticsService groupStatisticsService;

    @MockitoBean
    private GroupMatchesService groupMatchesService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ─── POST /api/v0/user-group/create ──────────────────────────────────────

    @Test
    @WithMockUser(username = "alice")
    void createGroup_success_returnsGroupResponse() throws Exception {
        UserGroupResponse response = UserGroupResponse.builder()
                .userGroupName("MyGroup").userGroupLeaderName("alice").build();
        when(userGroupService.createUserGroup(any(), eq("alice"))).thenReturn(response);

        mockMvc.perform(post("/api/v0/user-group/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserGroupRequest("MyGroup"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userGroupName", is("MyGroup")))
                .andExpect(jsonPath("$.userGroupLeaderName", is("alice")));
    }

    @Test
    @WithMockUser(username = "alice")
    void createGroup_leaderLimitExceeded_returns400() throws Exception {
        when(userGroupService.createUserGroup(any(), any()))
                .thenThrow(new IllegalArgumentException("at most 3 groups"));

        mockMvc.perform(post("/api/v0/user-group/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserGroupRequest("G"))))
                .andExpect(status().is4xxClientError());
    }

    // ─── GET /api/v0/user-group/all-users-group ───────────────────────────────

    @Test
    @WithMockUser(username = "alice")
    void getAllUserGroup_returnsList() throws Exception {
        List<UserGroupResponse> groups = List.of(
                UserGroupResponse.builder().userGroupName("G1").userGroupLeaderName("alice").build(),
                UserGroupResponse.builder().userGroupName("G2").userGroupLeaderName("bob").build()
        );
        when(userGroupService.findAllUserGroup("alice")).thenReturn(groups);

        mockMvc.perform(get("/api/v0/user-group/all-users-group"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].userGroupName", is("G1")));
    }

    // ─── GET /api/v0/user-group/find-group/{name} ─────────────────────────────

    @Test
    @WithMockUser
    void findGroup_found_returnsResponse() throws Exception {
        UserGroupResponse response = UserGroupResponse.builder()
                .userGroupName("G1").userGroupLeaderName("alice").build();
        when(userGroupService.findUserGroup("G1")).thenReturn(response);

        mockMvc.perform(get("/api/v0/user-group/find-group/G1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userGroupName", is("G1")));
    }

    @Test
    @WithMockUser
    void findGroup_notFound_returns400() throws Exception {
        when(userGroupService.findUserGroup("Missing"))
                .thenThrow(new IllegalArgumentException("Group not found"));

        mockMvc.perform(get("/api/v0/user-group/find-group/Missing"))
                .andExpect(status().is4xxClientError());
    }

    // ─── POST /api/v0/user-group/set-competitions ─────────────────────────────

    @Test
    @WithMockUser(username = "alice")
    void setCompetitions_success_returns200() throws Exception {
        doNothing().when(userGroupService).setGroupCompetitions(any(), eq("alice"));

        CreateGroupCompetitionsRequest req = CreateGroupCompetitionsRequest.builder()
                .groupName("G1")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .competitions(List.of("England:Premier League:PL"))
                .build();

        mockMvc.perform(post("/api/v0/user-group/set-competitions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "bob")
    void setCompetitions_notLeader_returns400() throws Exception {
        doThrow(new IllegalArgumentException("Only group leader"))
                .when(userGroupService).setGroupCompetitions(any(), any());

        CreateGroupCompetitionsRequest req = CreateGroupCompetitionsRequest.builder()
                .groupName("G1").startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(10))
                .competitions(List.of("A:B:C")).build();

        mockMvc.perform(post("/api/v0/user-group/set-competitions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    // ─── GET /api/v0/user-group/tournaments/{groupName} ───────────────────────

    @Test
    @WithMockUser
    void getTournaments_returnsList() throws Exception {
        List<GroupTournamentResponse> tournaments = List.of(
                GroupTournamentResponse.builder()
                        .id(1L).status("ACTIVE")
                        .startDate(LocalDate.now().minusDays(5))
                        .endDate(LocalDate.now().plusDays(25))
                        .competitions(List.of("England:PL"))
                        .build()
        );
        when(userGroupService.getGroupTournaments("G1")).thenReturn(tournaments);

        mockMvc.perform(get("/api/v0/user-group/tournaments/G1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
    }

    // ─── POST /api/v0/user-group/join-request ─────────────────────────────────

    @Test
    @WithMockUser(username = "bob")
    void createJoinRequest_success_returns200() throws Exception {
        doNothing().when(userGroupService).createJoinRequest(any(), eq("bob"));

        mockMvc.perform(post("/api/v0/user-group/join-request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinGroupRequest("G1"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void createJoinRequest_alreadyMember_returns400() throws Exception {
        doThrow(new IllegalArgumentException("already a member"))
                .when(userGroupService).createJoinRequest(any(), any());

        mockMvc.perform(post("/api/v0/user-group/join-request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinGroupRequest("G1"))))
                .andExpect(status().is4xxClientError());
    }

    // ─── GET /api/v0/user-group/join-requests/{groupName} ────────────────────

    @Test
    @WithMockUser
    void getPendingRequests_returnsList() throws Exception {
        List<GroupJoinRequestResponse> requests = List.of(
                GroupJoinRequestResponse.builder().userName("charlie").status("PENDING").build()
        );
        when(userGroupService.getPendingRequests("G1")).thenReturn(requests);

        mockMvc.perform(get("/api/v0/user-group/join-requests/G1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].userName", is("charlie")));
    }

    // ─── POST /api/v0/user-group/process-join-request ─────────────────────────

    @Test
    @WithMockUser(username = "alice")
    void processJoinRequest_accept_returns200() throws Exception {
        doNothing().when(userGroupService).processJoinRequest(any(), eq("alice"));

        mockMvc.perform(post("/api/v0/user-group/process-join-request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProcessJoinRequestRequest("bob", "G1", "ACCEPT"))))
                .andExpect(status().isOk());
    }

    // ─── GET /api/v0/user-group/members/{groupName} ───────────────────────────

    @Test
    @WithMockUser
    void getGroupMembers_returnsMemberList() throws Exception {
        when(userGroupService.getGroupMembers("G1")).thenReturn(List.of("alice", "bob"));

        mockMvc.perform(get("/api/v0/user-group/members/G1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0]", is("alice")));
    }

    // ─── GET /api/v0/user-group/ranking/{groupName} ───────────────────────────

    @Test
    @WithMockUser
    void getGroupRanking_withoutTournamentId_returns200() throws Exception {
        List<GroupRankingResponse> ranking = List.of(
                GroupRankingResponse.builder().rankingPosition(1L).userName("alice")
                        .correctPredictions(10L).predictionCount(15L).accuracyPercent(66).build()
        );
        when(groupStatisticsService.getGroupRanking("G1", null)).thenReturn(ranking);

        mockMvc.perform(get("/api/v0/user-group/ranking/G1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].userName", is("alice")))
                .andExpect(jsonPath("$[0].rankingPosition", is(1)));
    }

    @Test
    @WithMockUser
    void getGroupRanking_withTournamentId_passesIdToService() throws Exception {
        when(groupStatisticsService.getGroupRanking("G1", 5L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v0/user-group/ranking/G1").param("tournamentId", "5"))
                .andExpect(status().isOk());

        verify(groupStatisticsService).getGroupRanking("G1", 5L);
    }

    // ─── PUT /api/v0/user-group/tournament/update-dates ──────────────────────

    @Test
    @WithMockUser(username = "alice")
    void updateTournamentDates_success_returns200() throws Exception {
        doNothing().when(userGroupService).updateTournamentDates(any(), eq("alice"));

        UpdateTournamentDatesRequest req = UpdateTournamentDatesRequest.builder()
                .tournamentId(1L)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(40))
                .build();

        mockMvc.perform(put("/api/v0/user-group/tournament/update-dates")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ─── DELETE /api/v0/user-group/{groupName} ────────────────────────────────

    @Test
    @WithMockUser(username = "alice")
    void deleteGroup_success_returns200() throws Exception {
        doNothing().when(userGroupService).deleteGroup("G1", "alice");

        mockMvc.perform(delete("/api/v0/user-group/G1")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(userGroupService).deleteGroup("G1", "alice");
    }

    @Test
    @WithMockUser(username = "bob")
    void deleteGroup_notLeader_returns400() throws Exception {
        doThrow(new IllegalArgumentException("Only group leader"))
                .when(userGroupService).deleteGroup(any(), any());

        mockMvc.perform(delete("/api/v0/user-group/G1")
                        .with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    // ─── DELETE /api/v0/user-group/tournament/{id} ────────────────────────────

    @Test
    @WithMockUser(username = "alice")
    void deleteTournament_success_returns200() throws Exception {
        doNothing().when(userGroupService).deleteTournament(1L, "alice");

        mockMvc.perform(delete("/api/v0/user-group/tournament/1")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(userGroupService).deleteTournament(1L, "alice");
    }
}
