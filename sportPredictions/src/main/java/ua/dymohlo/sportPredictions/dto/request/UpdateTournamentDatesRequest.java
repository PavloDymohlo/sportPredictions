package ua.dymohlo.sportPredictions.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTournamentDatesRequest {
    private Long tournamentId;
    private LocalDate startDate;
    private LocalDate endDate;
}