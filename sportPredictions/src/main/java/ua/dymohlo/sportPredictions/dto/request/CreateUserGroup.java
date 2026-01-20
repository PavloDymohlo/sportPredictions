package ua.dymohlo.sportPredictions.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserGroup {
    private String userGroupName;
    private String userGroupLeaderName;
}
