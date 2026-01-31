package ua.dymohlo.sportPredictions.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ua.dymohlo.sportPredictions.dto.request.CreateUserGroupRequest;
import ua.dymohlo.sportPredictions.service.UserGroupService;

@Controller
@Slf4j
@RequiredArgsConstructor
public class GroupPageController {
    private final UserGroupService userGroupService;

    @GetMapping("/group/{groupName}")
    public String showGroupPage(@PathVariable String groupName, Model model) {
        log.info("Opening group page: {}", groupName);

        try {
            CreateUserGroupRequest group = userGroupService.findUserGroup(groupName);
            model.addAttribute("group", group);
            return "pages/group_page";
        } catch (IllegalArgumentException e) {
            log.error("Group not found: {}", groupName);
            return "redirect:/office-page";
        }
    }
}