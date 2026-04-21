package ua.dymohlo.sportPredictions.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ua.dymohlo.sportPredictions.dto.response.ChatMessageResponse;
import ua.dymohlo.sportPredictions.entity.GroupChat;
import ua.dymohlo.sportPredictions.repository.GroupChatRepository;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GroupChatService {

    private final GroupChatRepository groupChatRepository;

    private static final int PAGE_SIZE = 10;
    private static final int MAX_LENGTH = 500;
    private static final Pattern TEXT_ONLY = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}\\s]+$");

    public Page<ChatMessageResponse> getMessages(String groupName, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());
        return groupChatRepository.findByGroupName(groupName, pageable)
                .map(msg -> ChatMessageResponse.builder()
                        .username(msg.getUsername())
                        .message(msg.getMessage())
                        .createdAt(msg.getCreatedAt().toString())
                        .build());
    }

    public void sendMessage(String groupName, String username, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message is empty");
        }
        if (message.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Message too long");
        }
        if (!TEXT_ONLY.matcher(message).matches()) {
            throw new IllegalArgumentException("Only text characters allowed");
        }
        groupChatRepository.save(GroupChat.builder()
                .groupName(groupName)
                .username(username)
                .message(message.trim())
                .createdAt(LocalDateTime.now())
                .build());
    }
}
