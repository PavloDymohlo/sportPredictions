package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.dto.response.UserRankingResponse;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserRankingService {
    private final UserRepository userRepository;

    @Transactional
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
}
