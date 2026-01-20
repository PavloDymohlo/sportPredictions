package ua.dymohlo.sportPredictions.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionDTO implements Serializable {
    private String userName;
    private List<Object> predictions;
    private String matchDate;
}