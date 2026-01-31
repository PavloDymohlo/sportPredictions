package ua.dymohlo.sportPredictions.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupJoinRequestResponse {
    private String userName;
    private String status;
    private LocalDateTime createdAt;
}