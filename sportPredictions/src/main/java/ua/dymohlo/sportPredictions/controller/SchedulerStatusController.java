package ua.dymohlo.sportPredictions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.dymohlo.sportPredictions.dto.response.SchedulerStatusResponse;
import ua.dymohlo.sportPredictions.service.SchedulerStatusService;

@Tag(name = "Scheduler", description = "Daily data update scheduler status. Public endpoint.")
@RestController
@RequestMapping("/api/v0/scheduler")
@RequiredArgsConstructor
public class SchedulerStatusController {

    private final SchedulerStatusService schedulerStatusService;

    @Operation(summary = "Get scheduler status",
            description = "Returns the status of the daily data update job. " +
                    "Used by the frontend to determine whether cached data is still valid. " +
                    "Status values: COMPLETED, RUNNING, INCOMPLETE.")
    @ApiResponse(responseCode = "200", description = "Scheduler status with lastRunAt and nextRunAt timestamps.")
    @GetMapping("/status")
    public ResponseEntity<SchedulerStatusResponse> getStatus() {
        return ResponseEntity.ok(schedulerStatusService.getStatus());
    }
}
