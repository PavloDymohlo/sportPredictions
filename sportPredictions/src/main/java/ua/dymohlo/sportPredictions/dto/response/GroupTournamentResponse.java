package ua.dymohlo.sportPredictions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupTournamentResponse {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private List<String> competitions;
    private String winner;
}
