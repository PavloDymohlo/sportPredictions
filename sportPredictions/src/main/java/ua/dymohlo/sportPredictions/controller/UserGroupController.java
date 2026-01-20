package ua.dymohlo.sportPredictions.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ua.dymohlo.sportPredictions.dto.request.CreateUserGroup;
import ua.dymohlo.sportPredictions.service.UserGroupService;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v0/user-group")
public class UserGroupController {
    private final UserGroupService userGroupService;

    @PostMapping("/create")
    public CreateUserGroup createUserGroup(@RequestBody CreateUserGroup createUserGroup) {
        return userGroupService.createUserGroup(createUserGroup);
    }

    @GetMapping("/all-users-group/{username}")
    public List<CreateUserGroup> getAllUserGroup(@PathVariable String username) {
        return userGroupService.findAllUserGroup(username);
    }

    @GetMapping("/find-group/{userGroupName}")
    public CreateUserGroup findUserGroup(@PathVariable String userGroupName) {
        return userGroupService.findUserGroup(userGroupName);
    }
}
