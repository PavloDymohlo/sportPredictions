package ua.dymohlo.sportPredictions.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ua.dymohlo.sportPredictions.dto.response.UserGroupResponse;
import ua.dymohlo.sportPredictions.service.UserGroupService;

import java.security.Principal;

@Controller
@Slf4j
@RequiredArgsConstructor
public class GroupPageController {

    private final UserGroupService userGroupService;

    @GetMapping("/group/{groupName}")
    public String showGroupPage(@PathVariable String groupName, Model model, Principal principal) {
        log.info("Opening group page: {}", groupName);

        String username = principal.getName();
        if (!userGroupService.isUserMemberOfGroup(groupName, username)) {
            log.warn("User '{}' attempted to access group '{}' without membership.", username, groupName);
            return "redirect:/office-page";
        }

        UserGroupResponse group = userGroupService.findUserGroup(groupName);
        model.addAttribute("group", group);
        return "pages/group_page";
    }
}