package ua.dymohlo.sportPredictions.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ua.dymohlo.sportPredictions.entity.TelegramLinkToken;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.TelegramLinkTokenRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v0/telegram")
@RequiredArgsConstructor
public class TelegramController {

    private final UserRepository userRepository;
    private final TelegramLinkTokenRepository tokenRepository;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @GetMapping("/link-token")
    @Transactional
    public ResponseEntity<?> generateLinkToken(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = UUID.randomUUID().toString().replace("-", "");
        tokenRepository.save(new TelegramLinkToken(token, user, LocalDateTime.now()));

        return ResponseEntity.ok(Map.of(
                "token", token,
                "botUsername", botUsername,
                "connected", user.getTelegramChatId() != null
        ));
    }

    @DeleteMapping("/disconnect")
    @Transactional
    public ResponseEntity<?> disconnect(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTelegramChatId(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Telegram disconnected"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of("connected", user.getTelegramChatId() != null));
    }
}
