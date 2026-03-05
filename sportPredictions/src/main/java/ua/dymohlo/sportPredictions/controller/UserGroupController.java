package ua.dymohlo.sportPredictions.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.dymohlo.sportPredictions.dto.request.CreateGroupCompetitionsRequest;
import ua.dymohlo.sportPredictions.dto.request.CreateUserGroupRequest;
import ua.dymohlo.sportPredictions.dto.request.JoinGroupRequest;
import ua.dymohlo.sportPredictions.dto.request.ProcessJoinRequestRequest;
import ua.dymohlo.sportPredictions.dto.request.UpdateTournamentDatesRequest;
import ua.dymohlo.sportPredictions.dto.response.GroupJoinRequestResponse;
import ua.dymohlo.sportPredictions.dto.response.GroupRankingResponse;
import ua.dymohlo.sportPredictions.dto.response.GroupTournamentResponse;
import ua.dymohlo.sportPredictions.dto.response.GroupMatchesWithPredictionsResponse;
import ua.dymohlo.sportPredictions.dto.response.UserGroupResponse;
import ua.dymohlo.sportPredictions.service.GroupMatchesService;
import ua.dymohlo.sportPredictions.service.GroupStatisticsService;
import ua.dymohlo.sportPredictions.service.UserGroupService;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v0/user-group")
public class UserGroupController {

    private final UserGroupService userGroupService;
    private final GroupStatisticsService groupStatisticsService;
    private final GroupMatchesService groupMatchesService;

    @PostMapping("/create")
    public UserGroupResponse createUserGroup(@RequestBody CreateUserGroupRequest request,
                                             Authentication auth) {
        return userGroupService.createUserGroup(request, auth.getName());
    }

    @GetMapping("/all-users-group")
    public List<UserGroupResponse> getAllUserGroup(Authentication auth) {
        return userGroupService.findAllUserGroup(auth.getName());
    }

    @GetMapping("/find-group/{userGroupName}")
    public UserGroupResponse findUserGroup(@PathVariable String userGroupName) {
        return userGroupService.findUserGroup(userGroupName);
    }

    @PostMapping("/set-competitions")
    public ResponseEntity<Void> setGroupCompetitions(@RequestBody CreateGroupCompetitionsRequest request,
                                                     Authentication auth) {
        userGroupService.setGroupCompetitions(request, auth.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tournaments/{groupName}")
    public List<GroupTournamentResponse> getTournaments(@PathVariable String groupName) {
        return userGroupService.getGroupTournaments(groupName);
    }

    @PostMapping("/join-request")
    public ResponseEntity<Void> createJoinRequest(@RequestBody JoinGroupRequest request,
                                                  Authentication auth) {
        userGroupService.createJoinRequest(request, auth.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/join-requests/{groupName}")
    public List<GroupJoinRequestResponse> getPendingRequests(@PathVariable String groupName) {
        return userGroupService.getPendingRequests(groupName);
    }

    @PostMapping("/process-join-request")
    public ResponseEntity<Void> processJoinRequest(@RequestBody ProcessJoinRequestRequest request,
                                                   Authentication auth) {
        userGroupService.processJoinRequest(request, auth.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/members/{groupName}")
    public List<String> getGroupMembers(@PathVariable String groupName) {
        return userGroupService.getGroupMembers(groupName);
    }

    @GetMapping("/ranking/{groupName}")
    public List<GroupRankingResponse> getGroupRanking(
            @PathVariable String groupName,
            @RequestParam(required = false) Long tournamentId) {
        return groupStatisticsService.getGroupRanking(groupName, tournamentId);
    }

    @GetMapping("/matches-with-predictions/{groupName}")
    public ResponseEntity<GroupMatchesWithPredictionsResponse> getGroupMatchesWithPredictions(
            @PathVariable String groupName,
            @RequestParam("date") String date,
            @RequestParam(required = false) Long tournamentId) {
        return ResponseEntity.ok(groupMatchesService.getGroupMatchesWithPredictions(groupName, date, tournamentId));
    }

    @PutMapping("/tournament/update-dates")
    public ResponseEntity<Void> updateTournamentDates(@RequestBody UpdateTournamentDatesRequest request,
                                                      Authentication auth) {
        userGroupService.updateTournamentDates(request, auth.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupName}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String groupName, Authentication auth) {
        userGroupService.deleteGroup(groupName, auth.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tournament/{tournamentId}")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long tournamentId, Authentication auth) {
        userGroupService.deleteTournament(tournamentId, auth.getName());
        return ResponseEntity.ok().build();
    }
}