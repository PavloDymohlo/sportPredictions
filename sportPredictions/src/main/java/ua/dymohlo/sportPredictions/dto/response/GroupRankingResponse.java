package ua.dymohlo.sportPredictions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupRankingResponse {
    private long rankingPosition;
    private String userName;
    private long correctPredictions;
    private long predictionCount;
    private int accuracyPercent;
}