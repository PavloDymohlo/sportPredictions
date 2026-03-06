package ua.dymohlo.sportPredictions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Groups", description = "Group management, tournaments, join requests and rankings. Requires authentication.")
@SecurityRequirement(name = "cookieAuth")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v0/user-group")
public class UserGroupController {

    private final UserGroupService userGroupService;
    private final GroupStatisticsService groupStatisticsService;
    private final GroupMatchesService groupMatchesService;

    @Operation(summary = "Create a group", description = "Creates a new group. The authenticated user becomes the leader. A user can lead at most 3 groups.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Group created."),
            @ApiResponse(responseCode = "409", description = "Group name already taken or leader limit reached.")
    })
    @PostMapping("/create")
    public UserGroupResponse createUserGroup(@RequestBody CreateUserGroupRequest request,
                                             Authentication auth) {
        return userGroupService.createUserGroup(request, auth.getName());
    }

    @Operation(summary = "Get all groups for current user", description = "Returns all groups the authenticated user is a member of.")
    @ApiResponse(responseCode = "200", description = "List of groups.")
    @GetMapping("/all-users-group")
    public List<UserGroupResponse> getAllUserGroup(Authentication auth) {
        return userGroupService.findAllUserGroup(auth.getName());
    }

    @Operation(summary = "Find a group by name", description = "Searches for a group by exact name. Used before sending a join request.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Group found."),
            @ApiResponse(responseCode = "404", description = "Group not found.")
    })
    @GetMapping("/find-group/{userGroupName}")
    public UserGroupResponse findUserGroup(@PathVariable String userGroupName) {
        return userGroupService.findUserGroup(userGroupName);
    }

    @Operation(summary = "Create a new tournament for a group",
            description = "Leader only. Creates a new tournament with selected competitions and date range. " +
                    "A group can have at most 3 active or upcoming tournaments simultaneously.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tournament created."),
            @ApiResponse(responseCode = "403", description = "Not the group leader."),
            @ApiResponse(responseCode = "409", description = "Tournament limit reached.")
    })
    @PostMapping("/set-competitions")
    public ResponseEntity<Void> setGroupCompetitions(@RequestBody CreateGroupCompetitionsRequest request,
                                                     Authentication auth) {
        userGroupService.setGroupCompetitions(request, auth.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get all tournaments for a group", description = "Returns all tournaments (active, upcoming and finished) for the specified group.")
    @ApiResponse(responseCode = "200", description = "List of tournaments.")
    @GetMapping("/tournaments/{groupName}")
    public List<GroupTournamentResponse> getTournaments(@PathVariable String groupName) {
        return userGroupService.getGroupTournaments(groupName);
    }

    @Operation(summary = "Send a join request", description = "Sends a request to join a group. The leader must approve it. A user cannot send a duplicate request.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Join request sent."),
            @ApiResponse(responseCode = "409", description = "Already a member or pending request exists.")
    })
    @PostMapping("/join-request")
    public ResponseEntity<Void> createJoinRequest(@RequestBody JoinGroupRequest request,
                                                  Authentication auth) {
        userGroupService.createJoinRequest(request, auth.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get pending join requests", description = "Leader only. Returns all pending join requests for the group.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of pending requests."),
            @ApiResponse(responseCode = "403", description = "Not the group leader.")
    })
    @GetMapping("/join-requests/{groupName}")
    public List<GroupJoinRequestResponse> getPendingRequests(@PathVariable String groupName) {
        return userGroupService.getPendingRequests(groupName);
    }

    @Operation(summary = "Approve or reject a join request", description = "Leader only. Accepts or rejects a pending join request.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request processed."),
            @ApiResponse(responseCode = "403", description = "Not the group leader.")
    })
    @PostMapping("/process-join-request")
    public ResponseEntity<Void> processJoinRequest(@RequestBody ProcessJoinRequestRequest request,
                                                   Authentication auth) {
        userGroupService.processJoinRequest(request, auth.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get group members", description = "Returns a list of usernames of all group members.")
    @ApiResponse(responseCode = "200", description = "List of member usernames.")
    @GetMapping("/members/{groupName}")
    public List<String> getGroupMembers(@PathVariable String groupName) {
        return userGroupService.getGroupMembers(groupName);
    }

    @Operation(summary = "Get group ranking",
            description = "Returns ranking of group members. If tournamentId is provided, returns ranking for that specific tournament. Otherwise returns the overall group ranking.")
    @ApiResponse(responseCode = "200", description = "Ranked list of members.")
    @GetMapping("/ranking/{groupName}")
    public List<GroupRankingResponse> getGroupRanking(
            @PathVariable String groupName,
            @Parameter(description = "Optional tournament ID to filter ranking by a specific tournament")
            @RequestParam(required = false) Long tournamentId) {
        return groupStatisticsService.getGroupRanking(groupName, tournamentId);
    }

    @Operation(summary = "Get group matches with all members' predictions",
            description = "Returns matches for a given date within the tournament's competitions, along with each group member's prediction and result.")
    @ApiResponse(responseCode = "200", description = "Matches with predictions per member.")
    @GetMapping("/matches-with-predictions/{groupName}")
    public ResponseEntity<GroupMatchesWithPredictionsResponse> getGroupMatchesWithPredictions(
            @PathVariable String groupName,
            @Parameter(description = "Date in ISO format (yyyy-MM-dd)", example = "2025-03-10")
            @RequestParam("date") String date,
            @Parameter(description = "Optional tournament ID to scope the competitions")
            @RequestParam(required = false) Long tournamentId) {
        return ResponseEntity.ok(groupMatchesService.getGroupMatchesWithPredictions(groupName, date, tournamentId));
    }

    @Operation(summary = "Update tournament dates",
            description = "Leader only. Start date can only be changed before the tournament begins. End date can be changed until the final scoring night has passed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dates updated."),
            @ApiResponse(responseCode = "403", description = "Not the group leader."),
            @ApiResponse(responseCode = "400", description = "Date change not allowed at this stage.")
    })
    @PutMapping("/tournament/update-dates")
    public ResponseEntity<Void> updateTournamentDates(@RequestBody UpdateTournamentDatesRequest request,
                                                      Authentication auth) {
        userGroupService.updateTournamentDates(request, auth.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete a group", description = "Leader only. Permanently deletes the group and all associated data.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Group deleted."),
            @ApiResponse(responseCode = "403", description = "Not the group leader.")
    })
    @DeleteMapping("/{groupName}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String groupName, Authentication auth) {
        userGroupService.deleteGroup(groupName, auth.getName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete a tournament", description = "Leader only. Deletes a tournament and all its statistics.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tournament deleted."),
            @ApiResponse(responseCode = "403", description = "Not the group leader.")
    })
    @DeleteMapping("/tournament/{tournamentId}")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long tournamentId, Authentication auth) {
        userGroupService.deleteTournament(tournamentId, auth.getName());
        return ResponseEntity.ok().build();
    }
}