package com.echo.virtual_interview.controller.ai.chatMemory;

import com.echo.virtual_interview.mapper.InterviewDialogueMapper;
import com.echo.virtual_interview.model.entity.InterviewDialogue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MysqlInterviewMemory implements ChatMemory {


    private final InterviewDialogueMapper dialogueMapper;

    /**
     *
     * 此方法体被清空，不再执行任何数据库插入操作。
     * 满足了 ChatMemory 接口的要求，但实际上是一个空操作 (no-op)。
     */
    @Override
    public void add(String sessionId, List<Message> messages) {
        // 在其他逻辑IInterviewServiceImpl中的interviewProcess中实现了 ai面试时，对每轮对话的存储，所以这里不做存储
    }

    /**
     * 继续从数据库中读取历史记录。
     */
    @Override
    public List<Message> get(String sessionId, int lastN) {
        List<Message> messages = dialogueMapper.selectLastNMessages(sessionId, lastN).stream()
                .sorted(Comparator.comparingInt(InterviewDialogue::getSequence))
                .flatMap(d -> {
                    List<Message> result = new ArrayList<>();
                    if (d.getUserMessage() != null) {
                        result.add(new UserMessage(d.getUserMessage()));
                    }
                    if (d.getAiMessage() != null) {
                        result.add(new AssistantMessage(d.getAiMessage()));
                    }
                    return result.stream();
                })
                .collect(Collectors.toList());
        log.info("历史记忆："+messages.toString());
        return messages;
    }

    /**
     * 用于清除会话历史。
     */
    @Override
    public void clear(String sessionId) {
        dialogueMapper.deleteBySessionId(sessionId);
    }
}
