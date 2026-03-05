package ua.dymohlo.sportPredictions.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@Slf4j
public class HostPageController {
    @GetMapping("/host-page")
    public String showHostPage(Principal principal) {
        if (principal != null) {
            log.info("Already authenticated user '{}' redirected to office-page.", principal.getName());
            return "redirect:/office-page";
        }
        log.info("Host page accessed.");
        return "pages/host_page";
    }
}