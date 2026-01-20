package ua.dymohlo.sportPredictions.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_name")
    private String userName;
    @Column(name = "ranking_position")
    private long rankingPosition;
    @Column(name = "total_score")
    private long totalScore;
    @Column(name = "prediction_count")
    private long predictionCount;
    @Column(name = "percent_guessed_matches")
    private int percentGuessedMatches;
    @Column(name = "password")
    private String password;
    @Column(name = "last_predictions")
    private LocalDateTime lastPredictions;
}