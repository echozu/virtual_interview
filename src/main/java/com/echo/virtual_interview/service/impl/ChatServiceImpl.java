package com.echo.virtual_interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.controller.ai.ChatExpert;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.mapper.ChatsMapper;
import com.echo.virtual_interview.mapper.ChatsMessagesMapper;
import com.echo.virtual_interview.model.dto.chat.ChatDTO;
import com.echo.virtual_interview.model.entity.Chats;
import com.echo.virtual_interview.model.entity.ChatsMessages;
import com.echo.virtual_interview.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatsMapper chatsMapper;
    private final ChatsMessagesMapper messagesMapper;
    private final ChatExpert chatExpert;

    /**
     * 处理AI聊天请求（流式）
     * 该方法会自动判断是新会话还是已有会话。
     *
     * @param request 聊天请求，包含消息、可选的chatId和systemPrompt
     * @param userId  当前登录用户的ID
     * @return AI响应内容的流
     */
    @Transactional
    public Flux<String> processChatStream(ChatDTO.ChatRequest request, Long userId) {
        String chatId = request.getChatId();
        Chats existingChat = null;

        // 1. 如果前端提供了chatId，就去数据库验证其有效性。
        if (chatId != null && !chatId.isBlank()) {
            existingChat = chatsMapper.selectById(chatId);
            // 同时验证所有权和软删除状态
            if (existingChat != null && (!existingChat.getUserId().equals(userId) || existingChat.getIsDeleted() == 1)) {
                existingChat = null; // 如果不属于该用户或已被删除，则视作无效。
            }
        }

        // 2. 如果验证后，会话依然无效或不存在，则创建新会话。
        if (existingChat == null) {
            String newChatId = createNewChatSession(request, userId);
            if(request.getSystemPrompt().isEmpty() || request.getSystemPrompt().equals("null") ||request.getSystemPrompt().isBlank()){
                return chatExpert.doChatByStream(request.getMessage(), newChatId);
            }
            return chatExpert.doChatByStream(request.getMessage(), newChatId, request.getSystemPrompt());
        } else {
            // 3. 如果会话有效，则使用原有的chatId继续对话。
            if(request.getSystemPrompt().isEmpty() || request.getSystemPrompt().equals("null") ||request.getSystemPrompt().isBlank()){
                return chatExpert.doChatByStream(request.getMessage(), existingChat.getChatId());
            }
            return chatExpert.doChatByStream(request.getMessage(), existingChat.getChatId(), request.getSystemPrompt());
        }
    }

    /**
     * 创建一个新的会话
     */
    private String createNewChatSession(ChatDTO.ChatRequest request, Long userId) {
        Chats newChat = new Chats();
        String newChatId = request.getChatId();
        newChat.setChatId(newChatId);
        newChat.setUserId(userId);
        // 使用用户消息的前30个字符作为默认标题
        String title = request.getMessage().length() > 15
                ? request.getMessage().substring(0, 15) + "..."
                : request.getMessage();
        newChat.setTitle(title);
        newChat.setSystemPrompt(request.getSystemPrompt());
        // is_deleted 字段会使用数据库默认值 0

        chatsMapper.insert(newChat);
        return newChatId;
    }

    /**
     * 获取指定用户的所有会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    public List<ChatDTO.SessionResponse> getChatSessions(Long userId) {
        QueryWrapper<Chats> queryWrapper = new QueryWrapper<>();
        // 查询未被软删除的会话，并按更新时间倒序排列
        queryWrapper.lambda().eq(Chats::getUserId, userId)
                .eq(Chats::getIsDeleted, 0)
                .orderByDesc(Chats::getUpdatedAt);

        List<Chats> chats = chatsMapper.selectList(queryWrapper);

        return chats.stream().map(chat -> {
            ChatDTO.SessionResponse dto = new ChatDTO.SessionResponse();
            dto.setChatId(chat.getChatId());
            dto.setTitle(chat.getTitle());
            dto.setUpdatedAt(chat.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 根据会话ID获取所有聊天记录
     *
     * @param chatId 会话ID
     * @param userId 用户ID
     * @return 消息列表
     */
    public List<ChatDTO.MessageResponse> getChatMessages(String chatId, Long userId) {
        verifyChatOwnership(chatId, userId); // 验证所有权

        QueryWrapper<ChatsMessages> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ChatsMessages::getChatId, chatId).orderByAsc(ChatsMessages::getCreatedAt);

        List<ChatsMessages> messages = messagesMapper.selectList(queryWrapper);

        return messages.stream().map(msg -> {
            ChatDTO.MessageResponse dto = new ChatDTO.MessageResponse();
            dto.setRole(msg.getRole());
            dto.setContent(msg.getContent());
            dto.setCreatedAt(msg.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 更新会话标题
     *
     * @param chatId   会话ID
     * @param newTitle 新标题
     * @param userId   用户ID
     */
    @Transactional
    public void updateChatTitle(String chatId, String newTitle, Long userId) {
        Chats chat = verifyChatOwnership(chatId, userId);
        chat.setTitle(newTitle);
        chatsMapper.updateById(chat);
    }

    /**
     * (软)删除一个会话
     * @param chatId 会话ID
     * @param userId 用户ID
     */
    @Transactional
    public void deleteChatSession(String chatId, Long userId) {
        Chats chat = verifyChatOwnership(chatId, userId);
        chat.setIsDeleted(1); // 标记为已删除
        chatsMapper.updateById(chat);
    }

    /**
     * 验证会话是否属于指定用户
     *
     * @param chatId 会话ID
     * @param userId 用户ID
     * @return 如果验证通过，返回该会话实体
     * @throws RuntimeException 如果验证失败
     */
    private Chats verifyChatOwnership(String chatId, Long userId) {
        Chats chat = chatsMapper.selectById(chatId);
        if (chat == null || !chat.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"会话不存在或您没有权限访问该会话。");
        }
        return chat;
    }
}
