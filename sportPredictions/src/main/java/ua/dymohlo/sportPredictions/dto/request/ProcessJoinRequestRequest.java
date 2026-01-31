package ua.dymohlo.sportPredictions.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessJoinRequestRequest {
    private String userName;
    private String groupName;
    private String leaderName;
    private String action;
}