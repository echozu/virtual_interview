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

    @Override
    public void add(String sessionId, List<Message> messages) {
        int currentMaxSequence = dialogueMapper.getMaxSequence(sessionId);

        int seq = currentMaxSequence + 1;
        for (Message message : messages) {
            InterviewDialogue dialogue = new InterviewDialogue();
            dialogue.setSessionId(sessionId);
            dialogue.setSequence(seq++);
            dialogue.setTimestamp(LocalDateTime.now());

            if (message instanceof UserMessage userMsg) {
                dialogue.setUserMessage(userMsg.getText());
            } else if (message instanceof AssistantMessage aiMsg) {
                dialogue.setAiMessage(aiMsg.getText());
            } else {
                // 其他消息类型也可以支持
                dialogue.setUserMessage("[系统消息] " + message.getText());
            }

            dialogueMapper.insert(dialogue);
        }
    }

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


    @Override
    public void clear(String sessionId) {
        dialogueMapper.deleteBySessionId(sessionId);
    }
}
