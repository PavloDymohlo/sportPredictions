package ua.dymohlo.sportPredictions.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "group_user_statistics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "user_id"}))
public class GroupUserStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private GroupTournament groupTournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ranking_position", nullable = false)
    private long rankingPosition;

    @Column(name = "correct_predictions", nullable = false)
    private long correctPredictions;

    @Column(name = "prediction_count", nullable = false)
    private long predictionCount;

    @Column(name = "accuracy_percent", nullable = false)
    private int accuracyPercent;
}
