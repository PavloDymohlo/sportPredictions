package ua.dymohlo.sportPredictions.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "predictions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "match_date"}))
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "match_date", nullable = false)
    private LocalDate matchDate;

    @Column(name = "predictions_data", nullable = false, columnDefinition = "TEXT")
    private String predictionsData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}