package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ua.dymohlo.sportPredictions.entity.TelegramLinkToken;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TelegramLinkTokenRepository extends JpaRepository<TelegramLinkToken, String> {

    Optional<TelegramLinkToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM TelegramLinkToken t WHERE t.createdAt < :cutoff")
    void deleteExpiredTokens(LocalDateTime cutoff);
}
