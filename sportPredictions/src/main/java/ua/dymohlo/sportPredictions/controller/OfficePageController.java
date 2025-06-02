package ua.dymohlo.sportPredictions.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class OfficePageController {
    @GetMapping("/office-page")
    public String showOfficePage() {
        log.info("Office page accessed.");
        return "pages/office_page";
    }
}