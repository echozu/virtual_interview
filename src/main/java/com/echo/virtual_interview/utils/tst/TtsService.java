package com.echo.virtual_interview.utils.tst;


import com.echo.virtual_interview.constant.IflytekTtsProperties;
import com.echo.virtual_interview.model.dto.interview.audio.TtsRequest;
import com.echo.virtual_interview.model.dto.interview.audio.TtsResponse;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor // 使用Lombok自动注入final字段
public class TtsService {

    private final IflytekTtsProperties ttsProperties;
    private final OkHttpClient httpClient = new OkHttpClient.Builder().build();
    private final Gson gson = new Gson();

    private static final Path TTS_LOG_DIRECTORY = Paths.get("tts_audio_logs");

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(TTS_LOG_DIRECTORY)) {
                Files.createDirectories(TTS_LOG_DIRECTORY);
                log.info("成功创建TTS音频日志目录: {}", TTS_LOG_DIRECTORY.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("创建TTS音频日志目录失败！", e);
        }
    }

    // 【调试功能】保存音频到文件的方法
    private void saveTtsAudioToFile(String sessionId, String audioBase64) {
        if (audioBase64 == null || audioBase64.isEmpty()) {
            return;
        }
        // 使用会话ID和时间戳确保文件名唯一
        Path audioFile = TTS_LOG_DIRECTORY.resolve(sessionId + "_" + System.currentTimeMillis() + ".mp3");
        try {
            byte[] audioData = Base64.getDecoder().decode(audioBase64);
            Files.write(audioFile, audioData);
            log.info("TTS调试音频已保存至: {}", audioFile.toAbsolutePath());
        } catch (IllegalArgumentException e) {
            log.error("Base64解码失败: {}", e.getMessage());
        } catch (IOException e) {
            log.error("写入TTS调试音频文件失败", e);
        }
    }

    public void synthesize(String textToSynthesize,
                           Consumer<String> onAudioReceived,
                           Runnable onSynthesisComplete,
                           Consumer<String> onSynthesisFailed) {
        try {
            String authUrl = getAuthUrl();
            // 生成一个唯一的会话ID用于本次合成的日志和文件保存
            String synthesisSessionId = UUID.randomUUID().toString().substring(0, 8);
            log.info("发起TTS合成任务, SessionID: {}, URL: {}", synthesisSessionId, authUrl);

            Request request = new Request.Builder().url(authUrl).build();

            // 在回调中加入调试文件保存的逻辑
            TtsWebSocketListener listener = new TtsWebSocketListener(
                    synthesisSessionId, // 传递会话ID
                    textToSynthesize,
                    audioBase64 -> {
                        // 【保留调试功能】先保存文件，再调用外部回调
//                        saveTtsAudioToFile(synthesisSessionId, audioBase64);
                        onAudioReceived.accept(audioBase64);
                    },
                    onSynthesisComplete,
                    onSynthesisFailed
            );

            httpClient.newWebSocket(request, listener);

        } catch (Exception e) {
            log.error("创建讯飞TTS WebSocket连接时出错", e);
            onSynthesisFailed.accept("创建连接失败: " + e.getMessage());
        }
    }

    /**
     * 内部WebSocket监听器类，处理所有通信逻辑
     */
    private class TtsWebSocketListener extends WebSocketListener {
        private final String synthesisSessionId;
        private final String textToSynthesize;
        private final Consumer<String> onAudioReceived;
        private final Runnable onSynthesisComplete;
        private final Consumer<String> onSynthesisFailed;

        public TtsWebSocketListener(String synthesisSessionId, String textToSynthesize, Consumer<String> onAudioReceived, Runnable onSynthesisComplete, Consumer<String> onSynthesisFailed) {
            this.synthesisSessionId = synthesisSessionId;
            this.textToSynthesize = textToSynthesize;
            this.onAudioReceived = onAudioReceived;
            this.onSynthesisComplete = onSynthesisComplete;
            this.onSynthesisFailed = onSynthesisFailed;
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            log.info("[{}] 讯飞TTS WebSocket连接已打开，准备发送文本。", this.synthesisSessionId);

            TtsRequest ttsRequest = TtsRequest.defaultSingleRequest(
                    ttsProperties.getAppId(),
                    ttsProperties.getVcn(),
                    textToSynthesize
            );

            String requestJson = gson.toJson(ttsRequest);
            log.debug("[{}] 发送TTS请求: {}", this.synthesisSessionId, requestJson);
            webSocket.send(requestJson);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            TtsResponse response = gson.fromJson(text, TtsResponse.class);

            if (response.getHeader().getCode() != 0) {
                String errorMsg = String.format("[%s] TTS合成出错，错误码: %d, 信息: %s",
                        this.synthesisSessionId, response.getHeader().getCode(), response.getHeader().getMessage());
                log.error(errorMsg);
                onSynthesisFailed.accept(errorMsg);
                webSocket.close(1000, "synthesis_error");
                return;
            }

            if (response.getPayload() != null && response.getPayload().getAudio() != null) {
                String audioBase64 = response.getPayload().getAudio().getAudio();
                if (audioBase64 != null && !audioBase64.isEmpty()) {
                    onAudioReceived.accept(audioBase64);
                }
            }

            if (response.getHeader().getStatus() == 2) {
                log.info("[{}] TTS合成完成。", this.synthesisSessionId);
                onSynthesisComplete.run();
                webSocket.close(1000, "synthesis_complete");
            }
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            log.info("讯飞TTS WebSocket正在关闭: code={}, reason={}", code, reason);
            super.onClosing(webSocket, code, reason);
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            log.info("讯飞TTS WebSocket已关闭: code={}, reason={}", code, reason);
            super.onClosed(webSocket, code, reason);
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
            String errorMsg = "连接失败: " + t.getMessage();
            log.error("[{}] 讯飞TTS WebSocket连接失败: {}", this.synthesisSessionId, errorMsg, t);
            onSynthesisFailed.accept(errorMsg);
        }
    }


    /**
     * 生成带认证参数的URL (从官方Demo修改而来)
     * @return 认证后的URL
     * @throws Exception 签名过程中可能抛出异常
     */
    private String getAuthUrl() throws Exception {
        URL url = new URL("https://" + ttsProperties.getHost() + "/v1/private/mcd9m97e6");
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        String signatureOrigin = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(ttsProperties.getApiSecret().getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String sha = Base64.getEncoder().encodeToString(hexDigits);
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                ttsProperties.getApiKey(), "hmac-sha256", "host date request-line", sha);
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder()
                .addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8)))
                .addQueryParameter("date", date)
                .addQueryParameter("host", url.getHost())
                .build();
        return httpUrl.toString().replace("https://", "wss://").replace("http://", "ws://");
    }
}