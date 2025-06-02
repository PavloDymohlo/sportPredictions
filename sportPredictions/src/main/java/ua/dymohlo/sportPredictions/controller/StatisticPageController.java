package ua.dymohlo.sportPredictions.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class StatisticPageController {

    @GetMapping("/statistic-page")
    public String showStatisticPage() {
        log.info("Statistic page accessed.");
        return "pages/statistic_page";
    }
}