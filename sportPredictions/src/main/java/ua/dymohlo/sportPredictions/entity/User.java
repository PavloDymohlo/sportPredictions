package ua.dymohlo.sportPredictions.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_name", nullable = false, unique = true)
    private String userName;
    @Column(name = "ranking_position")
    private long rankingPosition;
    @Column(name = "total_score")
    private long totalScore;
    @Column(name = "prediction_count")
    private long predictionCount;
    @Column(name = "percent_guessed_matches")
    private int percentGuessedMatches;
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "last_predictions")
    private LocalDateTime lastPredictions;
    @Builder.Default
    @Column(name = "language", nullable = false, length = 5)
    private String language = "en";
    @Column(name = "telegram_chat_id")
    private Long telegramChatId;
}