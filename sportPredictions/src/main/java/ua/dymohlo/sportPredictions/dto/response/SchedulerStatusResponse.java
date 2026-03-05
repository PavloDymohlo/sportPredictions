package ua.dymohlo.sportPredictions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerStatusResponse {
    private String status;
    private String lastRunAt;
    private String nextRunAt;
}
