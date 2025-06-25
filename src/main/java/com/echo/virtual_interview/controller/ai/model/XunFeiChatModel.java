package com.echo.virtual_interview.controller.ai.model;

import com.echo.virtual_interview.constant.XunFeiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class XunFeiChatModel implements ChatModel {

    private final XunFeiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ChatResponse call(Prompt prompt) {
        try {
            // 1. 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", properties.getModel());

            List<Map<String, String>> messages = new ArrayList<>();
            for (Message message : prompt.getInstructions()) {
                messages.add(Map.of(
                        "role", message.getMessageType().name().toLowerCase(),
                        "content", message.getText()
                ));
            }
            requestBody.put("messages", messages);

            // 2. 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + properties.getKey());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 3. 发起请求
            ResponseEntity<String> response = restTemplate.exchange(
                    properties.getUrl(), HttpMethod.POST, entity, String.class
            );

            // 4. 解析响应
            String content = extractContent(response.getBody());
            AssistantMessage assistantMessage = new AssistantMessage(content);
            return new ChatResponse(List.of(new Generation(assistantMessage)));

        } catch (Exception e) {
            log.error("XunFeiChatModel Error", e);
            AssistantMessage assistantMessage = new AssistantMessage("【讯飞接口异常】" + e.getMessage());
            return new ChatResponse(List.of(new Generation(assistantMessage)));
        }
    }

    private String extractContent(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        return root.path("choices").get(0).path("message").path("content").asText();
    }
}
