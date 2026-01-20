package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.dymohlo.sportPredictions.configuration.PasswordEncoderConfig;
import ua.dymohlo.sportPredictions.dto.request.LoginInRequest;
import ua.dymohlo.sportPredictions.dto.request.RegisterRequest;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    public User register(RegisterRequest request) {
        String userName = request.getUserName();
        if (userRepository.findByUserName(userName).isPresent()) {
            throw new IllegalArgumentException("This username already exists!");
        }
        long startCount = 0;
        long userRankingPosition = calculateUserRankingPositionDuringRegistration();
        String passwordEncoded = PasswordEncoderConfig.encoderPassword(request.getPassword());

        User user = User.builder()
                .userName(userName)
                .password(passwordEncoded)
                .rankingPosition(userRankingPosition)
                .totalScore(startCount)
                .predictionCount(startCount)
                .percentGuessedMatches((int) startCount)
                .lastPredictions(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public String loginIn(LoginInRequest request) {
        Optional<User> user = userRepository.findByUserName(request.getUserName());

        if (user.isEmpty()) {
            throw new NoSuchElementException("invalid login");
        }

        if (!PasswordEncoderConfig.checkPassword(request.getPassword(), user.get().getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        return "Success";
    }
    private long calculateUserRankingPositionDuringRegistration() {
        return userRepository.count() + 1;
    }
}
