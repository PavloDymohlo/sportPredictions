package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.dymohlo.sportPredictions.dto.response.UserRankingResponse;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserRankingService {
    private final UserRepository userRepository;

    public void removeInactiveUsers() {
        List<User> users = userRepository.findAll();
        LocalDateTime expiredDate = LocalDateTime.now().minusDays(90);
        users.stream()
                .filter(user -> user.getLastPredictions() != null && user.getLastPredictions().isBefore(expiredDate))
                .forEach(user -> deleteUser(user.getUserName()));
        log.info("Search for passive users has taken place.");
    }


    public List<UserRankingResponse> getAllUsers() {
        List<User> users = userRepository.findAllRanked();
        updateRankingPositions(users);

        return users.stream()
                .map(user -> UserRankingResponse.builder()
                        .userName(user.getUserName())
                        .rankingPosition(user.getRankingPosition())
                        .totalScore(user.getTotalScore())
                        .predictionCount(user.getPredictionCount())
                        .percentGuessedMatches(user.getPercentGuessedMatches())
                        .build()
                )
                .toList();
    }

    private void updateRankingPositions(List<User> users) {
        long position = 1;
        for (User user : users) {
            user.setRankingPosition(position++);
        }
        userRepository.saveAll(users);
    }


    private void deleteUser(String username) {
        Optional<User> user = userRepository.findByUserName(username);
        user.ifPresent(userRepository::delete);
        log.info("Delete user with username: " + username);
    }
}
