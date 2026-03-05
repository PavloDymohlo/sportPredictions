package ua.dymohlo.sportPredictions.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.dymohlo.sportPredictions.dto.response.SchedulerStatusResponse;
import ua.dymohlo.sportPredictions.service.SchedulerStatusService;

@RestController
@RequestMapping("/api/v0/scheduler")
@RequiredArgsConstructor
public class SchedulerStatusController {

    private final SchedulerStatusService schedulerStatusService;

    @GetMapping("/status")
    public ResponseEntity<SchedulerStatusResponse> getStatus() {
        return ResponseEntity.ok(schedulerStatusService.getStatus());
    }
}
