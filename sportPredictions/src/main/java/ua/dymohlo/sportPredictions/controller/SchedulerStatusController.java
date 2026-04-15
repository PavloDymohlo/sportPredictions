package ua.dymohlo.sportPredictions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.dymohlo.sportPredictions.dto.response.SchedulerStatusResponse;
import ua.dymohlo.sportPredictions.scheduler.UpdateDataSchedule;
import ua.dymohlo.sportPredictions.service.SchedulerStatusService;

@Tag(name = "Scheduler", description = "Daily data update scheduler status. Public endpoint.")
@RestController
@RequestMapping("/api/v0/scheduler")
@RequiredArgsConstructor
public class SchedulerStatusController {

    private final SchedulerStatusService schedulerStatusService;
    private final UpdateDataSchedule updateDataSchedule;

    @Operation(summary = "Get scheduler status",
            description = "Returns the status of the daily data update job. " +
                    "Used by the frontend to determine whether cached data is still valid. " +
                    "Status values: COMPLETED, RUNNING, INCOMPLETE.")
    @ApiResponse(responseCode = "200", description = "Scheduler status with lastRunAt and nextRunAt timestamps.")
    @GetMapping("/status")
    public ResponseEntity<SchedulerStatusResponse> getStatus() {
        return ResponseEntity.ok(schedulerStatusService.getStatus());
    }

    @Operation(summary = "Manually trigger daily update")
    @ApiResponse(responseCode = "200", description = "Triggered successfully.")
    @ApiResponse(responseCode = "409", description = "Scheduler is already running.")
    @PostMapping("/trigger")
    public ResponseEntity<String> trigger() {
        SchedulerStatusResponse status = schedulerStatusService.getStatus();
        if ("RUNNING".equals(status.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Scheduler is already running");
        }
        new Thread(updateDataSchedule::runDailyUpdate).start();
        return ResponseEntity.ok("Daily update triggered");
    }
}
