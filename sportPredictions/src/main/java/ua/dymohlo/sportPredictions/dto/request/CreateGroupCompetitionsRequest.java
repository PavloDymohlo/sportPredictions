package ua.dymohlo.sportPredictions.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupCompetitionsRequest implements Serializable {
    private String groupName;
    private String leaderName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> competitions;
}