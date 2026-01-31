package ua.dymohlo.sportPredictions.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.dymohlo.sportPredictions.dto.request.CreateGroupCompetitionsRequest;
import ua.dymohlo.sportPredictions.dto.request.CreateUserGroupRequest;
import ua.dymohlo.sportPredictions.dto.request.JoinGroupRequest;
import ua.dymohlo.sportPredictions.dto.request.ProcessJoinRequestRequest;
import ua.dymohlo.sportPredictions.dto.response.GroupCompetitionsResponse;
import ua.dymohlo.sportPredictions.dto.response.GroupJoinRequestResponse;
import ua.dymohlo.sportPredictions.service.UserGroupService;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v0/user-group")
public class UserGroupController {
    private final UserGroupService userGroupService;

    @PostMapping("/create")
    public CreateUserGroupRequest createUserGroup(@RequestBody CreateUserGroupRequest createUserGroupRequest) {
        return userGroupService.createUserGroup(createUserGroupRequest);
    }

    @GetMapping("/all-users-group/{username}")
    public List<CreateUserGroupRequest> getAllUserGroup(@PathVariable String username) {
        return userGroupService.findAllUserGroup(username);
    }

    @GetMapping("/find-group/{userGroupName}")
    public CreateUserGroupRequest findUserGroup(@PathVariable String userGroupName) {
        return userGroupService.findUserGroup(userGroupName);
    }

    @PostMapping("/set-competitions")
    public ResponseEntity<String> setGroupCompetitions(@RequestBody CreateGroupCompetitionsRequest request) {
        userGroupService.setGroupCompetitions(request);
        return ResponseEntity.ok("Competitions set successfully");
    }

    @GetMapping("/get-competitions/{groupName}")
    public ResponseEntity<GroupCompetitionsResponse> getGroupCompetitions(@PathVariable String groupName) {
        GroupCompetitionsResponse response = userGroupService.getGroupCompetitions(groupName);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join-request")
    public ResponseEntity<String> createJoinRequest(@RequestBody JoinGroupRequest request) {
        userGroupService.createJoinRequest(request);
        return ResponseEntity.ok("Join request sent successfully");
    }

    @GetMapping("/join-requests/{groupName}")
    public ResponseEntity<List<GroupJoinRequestResponse>> getPendingRequests(@PathVariable String groupName) {
        List<GroupJoinRequestResponse> requests = userGroupService.getPendingRequests(groupName);
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/process-join-request")
    public ResponseEntity<String> processJoinRequest(@RequestBody ProcessJoinRequestRequest request) {
        userGroupService.processJoinRequest(request);
        return ResponseEntity.ok("Request processed successfully");
    }

    @GetMapping("/members/{groupName}")
    public ResponseEntity<List<String>> getGroupMembers(@PathVariable String groupName) {
        List<String> members = userGroupService.getGroupMembers(groupName);
        return ResponseEntity.ok(members);
    }
}
