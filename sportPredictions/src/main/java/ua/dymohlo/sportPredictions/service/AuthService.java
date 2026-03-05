package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ua.dymohlo.sportPredictions.dto.request.LoginInRequest;
import ua.dymohlo.sportPredictions.dto.request.RegisterRequest;
import ua.dymohlo.sportPredictions.dto.response.AuthResponse;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.UserRepository;
import ua.dymohlo.sportPredictions.secutity.JwtUtils;
import ua.dymohlo.sportPredictions.util.PasswordEncoderConfig;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String userName = request.getUserName();
        if (userRepository.findByUserName(userName).isPresent()) {
            throw new IllegalArgumentException("This username already exists!");
        }
        String passwordEncoded = PasswordEncoderConfig.encoderPassword(request.getPassword());

        User user = User.builder()
                .userName(userName)
                .password(passwordEncoded)
                .rankingPosition(nextRankingPosition())
                .totalScore(0)
                .predictionCount(0)
                .percentGuessedMatches(0)
                .lastPredictions(null)
                .build();

        User saved = userRepository.save(user);
        return toAuthResponse(saved);
    }

    public AuthResponse loginIn(LoginInRequest request) {
        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!PasswordEncoderConfig.checkPassword(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.builder()
                .userName(user.getUserName())
                .language(user.getLanguage())
                .token(jwtUtils.generateToken(user.getUserName()))
                .build();
    }

    private long nextRankingPosition() {
        return userRepository.count() + 1;
    }
}
