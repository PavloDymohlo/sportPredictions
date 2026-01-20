package ua.dymohlo.sportPredictions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRankingResponse {
    String userName;
    long rankingPosition;
    long totalScore;
    long predictionCount;
    int percentGuessedMatches;
}
