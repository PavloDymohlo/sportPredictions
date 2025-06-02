package ua.dymohlo.sportPredictions.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCompetitionListRequest implements Serializable {
    private String userName;
    private List<Map<String, String>> competitions;
}
