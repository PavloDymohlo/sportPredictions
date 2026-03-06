package ua.dymohlo.sportPredictions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.dymohlo.sportPredictions.dto.response.UserRankingResponse;
import ua.dymohlo.sportPredictions.service.UserRankingService;

import java.util.List;

@Tag(name = "Statistics", description = "Global player rankings. Requires authentication.")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v0/statistic")
public class StatisticController {
    private final UserRankingService userRankingService;

    @Operation(summary = "Get global player ranking",
            description = "Returns all users sorted by: correct predictions (desc), accuracy (desc), total predictions (desc).")
    @ApiResponse(responseCode = "200", description = "Ranked list of all users.")
    @GetMapping("/general")
    public List<UserRankingResponse> allUsersList() {
        return userRankingService.getAllUsers();
    }
}
