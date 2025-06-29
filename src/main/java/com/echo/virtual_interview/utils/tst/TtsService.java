package com.echo.virtual_interview.utils.tst;


import com.echo.virtual_interview.constant.IflytekTtsProperties;
import com.echo.virtual_interview.model.dto.interview.audio.TtsRequest;
import com.echo.virtual_interview.model.dto.interview.audio.TtsResponse;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    /**
     * 将文本合成为语音，并通过回调函数流式返回音频数据
     * @param textToSynthesize 需要合成的文本
     * @param onAudioReceived 接收到音频数据块时的回调函数。参数是Base64编码的音频数据字符串。
     * @param onSynthesisComplete 合成完成时的回调函数
     * @param onSynthesisFailed 合成失败时的回调函数
     */
    public void synthesize(String textToSynthesize,
                           Consumer<String> onAudioReceived,
                           Runnable onSynthesisComplete,
                           Consumer<String> onSynthesisFailed) {
        try {
            // 1. 获取认证URL
            String authUrl = getAuthUrl();
            log.info("讯飞TTS认证URL: {}", authUrl);

            // 2. 创建WebSocket请求
            Request request = new Request.Builder().url(authUrl).build();

            // 3. 创建WebSocket监听器
            TtsWebSocketListener listener = new TtsWebSocketListener(
                    textToSynthesize,
                    onAudioReceived,
                    onSynthesisComplete,
                    onSynthesisFailed
            );

            // 4. 发起连接
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
        private final String textToSynthesize;
        private final Consumer<String> onAudioReceived;
        private final Runnable onSynthesisComplete;
        private final Consumer<String> onSynthesisFailed;

        public TtsWebSocketListener(String textToSynthesize, Consumer<String> onAudioReceived, Runnable onSynthesisComplete, Consumer<String> onSynthesisFailed) {
            this.textToSynthesize = textToSynthesize;
            this.onAudioReceived = onAudioReceived;
            this.onSynthesisComplete = onSynthesisComplete;
            this.onSynthesisFailed = onSynthesisFailed;
        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            log.info("讯飞TTS WebSocket连接已打开，准备发送文本。");

            // 构建请求体
            TtsRequest ttsRequest = TtsRequest.defaultSingleRequest(
                    ttsProperties.getAppId(),
                    ttsProperties.getVcn(),
                    textToSynthesize
            );
            
            String requestJson = gson.toJson(ttsRequest);
            log.debug("发送TTS请求: {}", requestJson);
            webSocket.send(requestJson);
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            // log.trace("收到TTS消息: {}", text);
            TtsResponse response = gson.fromJson(text, TtsResponse.class);

            if (response.getHeader().getCode() != 0) {
                String errorMsg = "TTS合成出错，错误码: " + response.getHeader().getCode() + ", 信息: " + response.getHeader().getMessage();
                log.error(errorMsg);
                onSynthesisFailed.accept(errorMsg);
                webSocket.close(1000, "synthesis_error");
                return;
            }

            // 从payload中获取音频数据并调用回调
            if (response.getPayload() != null && response.getPayload().getAudio() != null) {
                String audioBase64 = response.getPayload().getAudio().getAudio();
                if (audioBase64 != null && !audioBase64.isEmpty()) {
                    onAudioReceived.accept(audioBase64);
                }
            }

            // 检查是否是最后一帧
            if (response.getHeader().getStatus() == 2) {
                log.info("TTS合成完成，会话ID: {}", response.getHeader().getSid());
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
            log.error("讯飞TTS WebSocket连接失败", t);
            onSynthesisFailed.accept("连接失败: " + t.getMessage());
            super.onFailure(webSocket, t, response);
        }
    }


    /**
     * 生成带认证参数的URL (从官方Demo修改而来)
     * @return 认证后的URL
     * @throws Exception 签名过程中可能抛出异常
     */
    private String getAuthUrl() throws Exception {
        URL url = new URL("https://" + ttsProperties.getHost() + "/v1/private/mcd9m97e6");

        // 时间使用GMT格式
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());

        // 构造签名原文
        String signatureOrigin = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";

        // HMAC-SHA256加密
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(ttsProperties.getApiSecret().getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        // 构造授权参数
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                ttsProperties.getApiKey(), "hmac-sha256", "host date request-line", sha);
        
        // 拼接URL
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder()
                .addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8)))
                .addQueryParameter("date", date)
                .addQueryParameter("host", url.getHost())
                .build();
        
        // 将https替换为wss
        return httpUrl.toString().replace("https://", "wss://").replace("http://", "ws://");
    }
}