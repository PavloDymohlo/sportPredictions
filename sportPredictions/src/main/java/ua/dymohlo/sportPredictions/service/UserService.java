package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.dymohlo.sportPredictions.repository.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void updateLanguage(String username, String language) {
        var user = userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setLanguage(language);
        userRepository.save(user);
        log.info("Language updated for user '{}': {}", username, language);
    }
}