package ua.dymohlo.sportPredictions.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ua.dymohlo.sportPredictions.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserName(String userName);

    Optional<User> findByTelegramChatId(Long telegramChatId);

    java.util.List<User> findAllByTelegramChatIdNotNull();

    @Query("""
            SELECT u FROM User u
            ORDER BY
                u.totalScore DESC,
                u.percentGuessedMatches DESC,
                u.predictionCount DESC""")
    List<User> findAllRanked();
}
