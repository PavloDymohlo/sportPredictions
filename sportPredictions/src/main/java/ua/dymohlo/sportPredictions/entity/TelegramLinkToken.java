package ua.dymohlo.sportPredictions.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "telegram_link_tokens")
public class TelegramLinkToken {

    @Id
    @Column(name = "token", length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return createdAt.isBefore(LocalDateTime.now().minusMinutes(10));
    }
}
