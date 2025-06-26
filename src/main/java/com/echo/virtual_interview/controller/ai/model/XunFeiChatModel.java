package com.echo.virtual_interview.controller.ai.model;

import com.echo.virtual_interview.constant.XunFeiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class XunFeiChatModel implements ChatModel, StreamingChatModel {

    private final XunFeiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ChatResponse call(Prompt prompt) {
        try {
            Map<String, Object> requestBody = buildRequestBody(prompt, false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + properties.getKey());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(properties.getUrl(), entity, String.class);

            String content = extractContent(response.getBody());
            return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
        } catch (Exception e) {
            log.error("XunFeiChatModel Error", e);
            return new ChatResponse(List.of(new Generation(new AssistantMessage("【讯飞接口异常】" + e.getMessage()))));
        }
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.create(emitter -> {
            try {
                Map<String, Object> requestBody = buildRequestBody(prompt, true);

                HttpURLConnection connection = (HttpURLConnection) new URL(properties.getUrl()).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + properties.getKey());
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String requestJson = objectMapper.writeValueAsString(requestBody);
                connection.getOutputStream().write(requestJson.getBytes(StandardCharsets.UTF_8));

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!StringUtils.hasText(line)) continue;
                        if (line.startsWith("data:")) {
                            String json = line.substring(5).trim();
                            if (json.equals("[DONE]")) break;
                            String content = extractContent(json);
                            emitter.next(new ChatResponse(List.of(new Generation(new AssistantMessage(content)))));
                        }
                    }
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("XunFeiChatModel Stream Error", e);
                emitter.error(e);
            }
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    private Map<String, Object> buildRequestBody(Prompt prompt, boolean stream) {
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
        if (stream) {
            requestBody.put("stream", true);
        }
        return requestBody;
    }

    private String extractContent(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        // 兼容流式响应结构：choices[0].delta.content
        JsonNode deltaContent = root.path("choices").get(0).path("delta").path("content");
        if (!deltaContent.isMissingNode()) {
            return deltaContent.asText(); // 流式时走这个
        }

        // 非流式结构（兼容旧逻辑）
        return root.path("choices").get(0).path("message").path("content").asText();
    }

}
