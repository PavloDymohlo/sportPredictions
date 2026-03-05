package ua.dymohlo.sportPredictions.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ua.dymohlo.sportPredictions.entity.TelegramLinkToken;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.TelegramLinkTokenRepository;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class TelegramBotService implements DisposableBean {

    private final WebClient webClient;
    private final TelegramLinkTokenRepository tokenRepository;
    private final UserRepository userRepository;

    private volatile boolean running = true;
    private Thread pollingThread;

    public TelegramBotService(
            WebClient.Builder webClientBuilder,
            TelegramLinkTokenRepository tokenRepository,
            UserRepository userRepository,
            @Value("${telegram.bot.token}") String botToken) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.telegram.org/bot" + botToken)
                .build();
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void startPolling() {
        pollingThread = new Thread(this::poll, "telegram-polling");
        pollingThread.setDaemon(true);
        pollingThread.start();
        log.info("Telegram bot long polling started");
    }

    @SuppressWarnings("unchecked")
    private void poll() {
        long offset = 0;
        while (running) {
            try {
                Map<?, ?> response = webClient.get()
                        .uri("/getUpdates?offset={offset}&timeout=30", offset)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block(Duration.ofSeconds(40));

                if (response == null || !Boolean.TRUE.equals(response.get("ok"))) continue;

                List<Map<String, Object>> updates = (List<Map<String, Object>>) response.get("result");
                for (Map<String, Object> update : updates) {
                    long updateId = ((Number) update.get("update_id")).longValue();
                    offset = updateId + 1;
                    processUpdate(update);
                }
            } catch (Exception e) {
                if (running) {
                    log.warn("Telegram polling error: {}", e.getMessage());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processUpdate(Map<String, Object> update) {
        try {
            Map<String, Object> message = (Map<String, Object>) update.get("message");
            if (message == null) return;

            Map<String, Object> chat = (Map<String, Object>) message.get("chat");
            long chatId = ((Number) chat.get("id")).longValue();
            String text = (String) message.get("text");
            if (text == null) return;

            if (text.startsWith("/start ")) {
                handleStart(chatId, text.substring(7).trim());
            } else if (text.equals("/stop")) {
                handleStop(chatId);
            }
        } catch (Exception e) {
            log.warn("Failed to process Telegram update: {}", e.getMessage());
        }
    }

    private void handleStart(long chatId, String token) {
        Optional<TelegramLinkToken> linkTokenOpt = tokenRepository.findByToken(token);
        if (linkTokenOpt.isEmpty() || linkTokenOpt.get().isExpired()) {
            linkTokenOpt.ifPresent(tokenRepository::delete);
            sendMessage(chatId, "❌ Link expired or invalid. Please generate a new one on the website.");
            return;
        }

        TelegramLinkToken linkToken = linkTokenOpt.get();
        User user = linkToken.getUser();
        user.setTelegramChatId(chatId);
        userRepository.save(user);
        tokenRepository.delete(linkToken);

        String msg = "uk".equals(user.getLanguage())
                ? "✅ Telegram успішно підключено! Ви отримуватимете сповіщення про нові дані."
                : "✅ Telegram connected! You will receive notifications when new data is available.";
        sendMessage(chatId, msg);
        log.info("User '{}' linked Telegram chat {}", user.getUserName(), chatId);
    }

    private void handleStop(long chatId) {
        userRepository.findByTelegramChatId(chatId).ifPresentOrElse(user -> {
            user.setTelegramChatId(null);
            userRepository.save(user);
            String msg = "uk".equals(user.getLanguage())
                    ? "🔕 Telegram відключено. Ви більше не отримуватимете сповіщень."
                    : "🔕 Telegram disconnected. You will no longer receive notifications.";
            sendMessage(chatId, msg);
            log.info("User '{}' unlinked Telegram", user.getUserName());
        }, () -> sendMessage(chatId, "You are not connected to any account."));
    }

    public void sendMessage(long chatId, String text) {
        try {
            webClient.post()
                    .uri("/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("chat_id", chatId, "text", text))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(10));
        } catch (Exception e) {
            log.warn("Failed to send Telegram message to chat {}: {}", chatId, e.getMessage());
        }
    }

    @Override
    public void destroy() {
        running = false;
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
    }
}
