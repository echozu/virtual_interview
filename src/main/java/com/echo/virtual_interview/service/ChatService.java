package com.echo.virtual_interview.service;

import com.echo.virtual_interview.model.dto.chat.ChatDTO;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {
    Flux<String> processChatStream(ChatDTO.ChatRequest request, Long currentUserId);

    List<ChatDTO.SessionResponse> getChatSessions(Long currentUserId);

    List<ChatDTO.MessageResponse> getChatMessages(String chatId, Long currentUserId);

    void updateChatTitle(String chatId, String newTitle, Long currentUserId);

    void deleteChatSession(String chatId, Long currentUserId);
}
