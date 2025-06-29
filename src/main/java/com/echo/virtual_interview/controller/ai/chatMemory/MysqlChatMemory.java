package com.echo.virtual_interview.controller.ai.chatMemory;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.echo.virtual_interview.mapper.ChatsMapper;
import com.echo.virtual_interview.mapper.ChatsMessagesMapper;
import com.echo.virtual_interview.model.entity.Chats;
import com.echo.virtual_interview.model.entity.ChatsMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MysqlChatMemory implements ChatMemory {

    private final ChatsMessagesMapper messagesMapper;
    private final ChatsMapper chatsMapper;

    /**
     * 将消息列表添加到指定会话中。
     * 每次添加后，都会更新父会话的 `updated_at` 字段。
     * @param chatId 会话ID，对应于`chats`表的`chatId`
     * @param messages Spring AI 的消息对象列表
     */
    @Override
    @Transactional
    public void add(String chatId, List<Message> messages) {
        for (Message message : messages) {
            ChatsMessages chatsMessage = new ChatsMessages();
            chatsMessage.setChatId(chatId);
            chatsMessage.setRole(message.getMessageType().getValue()); // "user" or "ai"
            chatsMessage.setContent(message.getText());
            chatsMessage.setCreatedAt(LocalDateTime.now());
            messagesMapper.insert(chatsMessage);
        }

        // 更新父会话的 `updated_at` 字段，以确保会话列表排序正确
        Chats chat = chatsMapper.selectById(chatId);
        if (chat != null) {
            chat.setUpdatedAt(LocalDateTime.now());
            chatsMapper.updateById(chat);
        } else {
            log.warn("Attempted to add messages to a non-existent chat session with chatId: {}", chatId);
            // 这里可以根据业务需求决定是否抛出异常
        }
    }

    /**
     * 从指定会话中获取最近的N条消息。
     * @param chatId 会话ID
     * @param lastN 要获取的消息数量。注意：如果 lastN=0，表示获取所有历史记录。
     * @return Spring AI 的消息对象列表
     */
    @Override
    public List<Message> get(String chatId, int lastN) {
        if (lastN <= 0) {
            // 如果 lastN 为0或负数, mybatis limit 会报错，这里处理为查询所有记录
            // 如果不希望查询所有，可以设定一个最大值，例如1000
            QueryWrapper<ChatsMessages> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("chatId", chatId).orderByAsc("created_at");
            return messagesMapper.selectList(queryWrapper)
                    .stream()
                    .map(this::convertToSpringAiMessage)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        List<ChatsMessages> dbMessages = messagesMapper.selectLastNMessages(chatId, lastN);

        List<Message> result = dbMessages.stream()
                .map(this::convertToSpringAiMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("从数据库加载历史记忆 (ChatID: {}): {} 条", chatId, result.size());
        return result;
    }

    /**
     * 将数据库实体 ChatsMessage 转换为 Spring AI 的 Message 对象。
     */
    private Message convertToSpringAiMessage(ChatsMessages dbMessage) {
        if (dbMessage == null || dbMessage.getRole() == null) {
            return null;
        }
        MessageType messageType = MessageType.fromValue(dbMessage.getRole());
        switch (messageType) {
            case USER:
                return new UserMessage(dbMessage.getContent());
            case ASSISTANT:
                // 在您的数据库schema中，AI的角色可能存为 'assistant'
                // MessageType.fromValue("assistant") 会正确处理
                return new AssistantMessage(dbMessage.getContent());
            default:
                log.warn("Unsupported message role type from DB: {}", dbMessage.getRole());
                return null; // 或者可以返回一个 SystemMessage
        }
    }

    /**
     * 清除指定会话的所有消息记录。
     * 注意：这只会删除 `chats_messages` 表中的记录，不会删除 `chats` 表中的会话本身。
     * @param chatId 会话ID
     */
    @Override
    public void clear(String chatId) {
        QueryWrapper<ChatsMessages> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chatId", chatId);
        int deletedRows = messagesMapper.delete(queryWrapper);
        log.info("Cleared chat memory for chatId: {}. Deleted {} messages.", chatId, deletedRows);
    }
}
