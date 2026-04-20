package ua.dymohlo.sportPredictions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ua.dymohlo.sportPredictions.dto.request.BugReportRequest;
import ua.dymohlo.sportPredictions.service.TelegramBotService;

import java.util.Map;

@Tag(name = "Bug Report", description = "Send a bug report to the admin via Telegram.")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequestMapping("/api/v0/bug-report")
@RequiredArgsConstructor
public class BugReportController {

    @Autowired(required = false)
    private TelegramBotService telegramBotService;

    @Value("${telegram.admin.chat-id:0}")
    private long adminChatId;

    @Operation(summary = "Submit a bug report",
            description = "Sends the user's bug report as a Telegram message to the admin. Requires authentication.")
    @PostMapping
    public ResponseEntity<?> submitBugReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody BugReportRequest request) {

        String message = request.getMessage();
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is empty"));
        }

        if (telegramBotService == null) {
            return ResponseEntity.ok(Map.of("message", "Report sent"));
        }

        String text = "\uD83D\uDC1B Bug Report\nFrom: " + userDetails.getUsername() + "\n\n" + message;
        telegramBotService.sendMessage(adminChatId, text);
        return ResponseEntity.ok(Map.of("message", "Report sent"));
    }
}
