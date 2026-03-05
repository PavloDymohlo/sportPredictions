package ua.dymohlo.sportPredictions.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateLanguageRequest {
    @NotBlank
    private String language;
}