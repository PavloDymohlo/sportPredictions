package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ua.dymohlo.sportPredictions.entity.User;
import ua.dymohlo.sportPredictions.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "telegram.enabled", havingValue = "true")
public class TelegramNotificationService {

    private final UserRepository userRepository;
    private final TelegramBotService telegramBotService;

    private static final String MSG_EN = """
            ✅ New data is available!

            Yesterday's results have been processed. Check your scores and make new predictions for upcoming matches!
            """;

    private static final String MSG_UK = """
            ✅ Нові дані доступні!

            Результати вчорашнього дня опрацьовано. Перевірте свої бали та поставте нові прогнози!
            """;

    public void notifyAllUsers() {
        List<User> users = userRepository.findAllByTelegramChatIdNotNull();
        if (users.isEmpty()) {
            log.info("No Telegram users to notify");
            return;
        }

        log.info("Sending Telegram notifications to {} users", users.size());
        int success = 0;
        for (User user : users) {
            try {
                String text = buildMessage(user.getLanguage());
                telegramBotService.sendMessage(user.getTelegramChatId(), text);
                success++;
            } catch (Exception e) {
                log.warn("Failed to notify user '{}'", user.getUserName(), e);
            }
        }
        log.info("Telegram notifications sent: {}/{}", success, users.size());
    }

    private String buildMessage(String language) {
        String body = "uk".equals(language) ? MSG_UK : MSG_EN;
        return body;
    }
}
