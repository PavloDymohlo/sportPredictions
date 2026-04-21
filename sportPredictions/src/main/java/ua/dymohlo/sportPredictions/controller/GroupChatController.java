package ua.dymohlo.sportPredictions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.dymohlo.sportPredictions.dto.request.SendChatMessageRequest;
import ua.dymohlo.sportPredictions.dto.response.ChatMessageResponse;
import ua.dymohlo.sportPredictions.service.GroupChatService;

import java.util.Map;

@Tag(name = "Group Chat", description = "Group chat messages.")
@SecurityRequirement(name = "cookieAuth")
@RestController
@RequestMapping("/api/v0/chat")
@RequiredArgsConstructor
public class GroupChatController {

    private final GroupChatService groupChatService;

    @Operation(summary = "Get chat messages", description = "Returns paginated chat messages for the group, newest first.")
    @GetMapping("/{groupName}")
    public Page<ChatMessageResponse> getMessages(
            @PathVariable String groupName,
            @RequestParam(defaultValue = "0") int page) {
        return groupChatService.getMessages(groupName, page);
    }

    @Operation(summary = "Send a chat message", description = "Saves a text message to the group chat.")
    @PostMapping("/{groupName}")
    public ResponseEntity<?> sendMessage(
            @PathVariable String groupName,
            @RequestBody SendChatMessageRequest request,
            Authentication auth) {
        try {
            groupChatService.sendMessage(groupName, auth.getName(), request.getMessage());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
