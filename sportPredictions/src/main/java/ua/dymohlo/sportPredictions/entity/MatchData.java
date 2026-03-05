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
@Table(name = "match_data")
public class MatchData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_date", nullable = false, unique = true)
    private LocalDate matchDate;

    @Column(name = "data", nullable = false, columnDefinition = "TEXT")
    private String matchesJson;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;
}