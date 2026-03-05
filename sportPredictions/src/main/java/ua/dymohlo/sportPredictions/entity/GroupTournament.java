package ua.dymohlo.sportPredictions.entity;

import jakarta.persistence.*;
import lombok.*;
import ua.dymohlo.sportPredictions.enums.CompetitionStatus;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "group_tournaments")
public class GroupTournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private UserGroup userGroup;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "finish_date")
    private LocalDate finishDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CompetitionStatus status = CompetitionStatus.NOT_STARTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_user_id")
    private User winner;

    @Column(name = "last_processed_date")
    private LocalDate lastProcessedDate;
}
