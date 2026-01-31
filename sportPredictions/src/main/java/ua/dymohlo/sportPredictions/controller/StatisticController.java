package ua.dymohlo.sportPredictions.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.dymohlo.sportPredictions.dto.response.UserRankingResponse;
import ua.dymohlo.sportPredictions.service.UserRankingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v0/statistic")
public class StatisticController {
    private final UserRankingService userRankingService;

    @GetMapping("/general")
    public List<UserRankingResponse> allUsersList() {
        return userRankingService.getAllUsers();
    }
}
