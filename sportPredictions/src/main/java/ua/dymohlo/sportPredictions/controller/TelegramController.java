package ua.dymohlo.sportPredictions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Telegram", description = "Telegram bot connection for daily data update notifications. Requires authentication.")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequestMapping("/api/v0/telegram")
@RequiredArgsConstructor
public class TelegramController {

    private final UserRepository userRepository;
    private final TelegramLinkTokenRepository tokenRepository;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Operation(summary = "Generate Telegram link token",
            description = "Generates a one-time token (valid 10 minutes) used to connect the user's account to the Telegram bot. " +
                    "Redirect the user to: https://t.me/{botUsername}?start={token}. " +
                    "When the user presses Start in the bot, the accounts are linked automatically.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Returns token, botUsername and current connected status.")
    })
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

    @Operation(summary = "Disconnect Telegram", description = "Unlinks the Telegram account from the user. The user will no longer receive notifications.")
    @ApiResponse(responseCode = "200", description = "Telegram disconnected.")
    @DeleteMapping("/disconnect")
    @Transactional
    public ResponseEntity<?> disconnect(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTelegramChatId(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Telegram disconnected"));
    }

    @Operation(summary = "Get Telegram connection status", description = "Returns whether the current user has linked their Telegram account.")
    @ApiResponse(responseCode = "200", description = "Returns { connected: true/false }.")
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of("connected", user.getTelegramChatId() != null));
    }
}
