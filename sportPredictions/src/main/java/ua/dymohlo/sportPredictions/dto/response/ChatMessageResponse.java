package ua.dymohlo.sportPredictions.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageResponse {
    private String username;
    private String message;
    private String createdAt;
}
