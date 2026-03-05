package ua.dymohlo.sportPredictions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompetitionResponse {
    private Long id;
    private String country;
    private String name;
    private String code;
}
