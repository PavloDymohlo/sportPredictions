package ua.dymohlo.sportPredictions.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString(exclude = {"users", "competitions"})
@Table(name = "user_groups", uniqueConstraints = @UniqueConstraint(columnNames = "group_name"))
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_name", nullable = false, unique = true)
    private String groupName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_leader_id", nullable = false)
    private User groupLeader;

    @ManyToMany
    @JoinTable(
            name = "user_group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> users;

    @ManyToMany
    @JoinTable(
            name = "group_competitions",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "competition_id")
    )
    private List<Competition> competitions;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "finish_date")
    private LocalDateTime finishDate;
}
