package ua.dymohlo.sportPredictions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupMatchesWithPredictionsResponse {
    private String groupName;
    private String date;
    private List<String> members;
    private List<CompetitionMatches> competitions;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompetitionMatches {
        private String country;
        private String tournament;
        private List<MatchWithPredictions> matches;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MatchWithPredictions {
        private String homeTeam;
        private String awayTeam;
        private String homeScore;
        private String awayScore;
        private Map<String, UserPrediction> predictions;
    }
}