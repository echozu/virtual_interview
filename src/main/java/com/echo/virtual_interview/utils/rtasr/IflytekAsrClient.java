package com.echo.virtual_interview.utils.rtasr;

import com.echo.virtual_interview.iflytek.util.EncryptUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.function.Consumer;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;

@Slf4j
public class IflytekAsrClient extends WebSocketClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Consumer<String> onMessageCallback;
    private final Runnable onCloseCallback;

    public IflytekAsrClient(URI serverUri, Draft draft, Consumer<String> onMessageCallback, Runnable onCloseCallback) {
        super(serverUri, draft);
        this.onMessageCallback = onMessageCallback;
        this.onCloseCallback = onCloseCallback;
        if (serverUri.toString().startsWith("wss")) {
            this.setSocketFactory(createSslSocketFactory());
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("[讯飞ASR] 连接建立成功: {}", handshakedata.getHttpStatusMessage());
    }

    @Override
    public void onMessage(String message) {
//        log.info("[讯飞ASR] 收到原始消息: {}", message);

        try {
            JsonNode messageNode = objectMapper.readTree(message);
            String action = messageNode.path("action").asText();
            String sid = messageNode.path("sid").asText();

            if (Objects.equals("started", action)) {
                log.info("[讯飞ASR] 握手成功, SID: {}", sid);
            } else if (Objects.equals("result", action)) {
                String content = parseContent(messageNode.path("data").asText());
                if (!content.isEmpty()) {
                    onMessageCallback.accept(content);
                }
            } else if (Objects.equals("error", action)) {
                log.error("[讯飞ASR] 发生错误, SID: {}, 错误码: {}, 描述: {}",
                        sid, messageNode.path("code").asText(), messageNode.path("desc").asText());
                this.close();
            }
        } catch (Exception e) {
            log.error("[讯飞ASR] 解析消息失败: {}", message, e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("[讯飞ASR] 连接关闭. Code: {}, Reason: {}, Remote: {}", code, reason, remote);
        if(onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    @Override
    public void onError(Exception ex) {
        log.error("[讯飞ASR] 连接发生异常", ex);
    }

    @Override
    public void onMessage(ByteBuffer bytes) {

        try {
            String message = new String(bytes.array(), "UTF-8");
            log.warn("[讯飞ASR] 服务端返回非预期的二进制消息: {}", message);
        } catch (UnsupportedEncodingException e) {
            log.error("[讯飞ASR] 解析二进制消息失败", e);
        }
    }

    public void sendAudio(byte[] audioData) {
        if (isOpen()) {
//            log.info("[讯飞ASR] 正在发送 {} bytes 的音频数据到 {}", audioData.length, this.getURI());

            this.send(audioData);
        } else {
            log.warn("[讯飞ASR] 连接已关闭，无法发送音频数据");
        }
    }

    public void sendEnd() {
        if (isOpen()) {
            log.info("[讯飞ASR] 发送结束帧");
            this.send("{\"end\": true}");
        }
    }

    private String parseContent(String dataJson) {
        try {
            JsonNode dataNode = objectMapper.readTree(dataJson);
            // 我们只关心最终结果
            if (dataNode.path("cn").path("st").path("type").asInt() == 0) {
                StringBuilder resultBuilder = new StringBuilder();
                JsonNode rtArray = dataNode.path("cn").path("st").path("rt");
                for (JsonNode rt : rtArray) {
                    JsonNode wsArray = rt.path("ws");
                    for (JsonNode ws : wsArray) {
                        JsonNode cwArray = ws.path("cw");
                        for (JsonNode cw : cwArray) {
                            resultBuilder.append(cw.path("w").asText());
                        }
                    }
                }
                String finalResult = resultBuilder.toString();
                if (!finalResult.isEmpty()) {
                    log.info("[讯飞ASR] 收到最终识别结果: {}", finalResult);
                    return finalResult;
                }
            }
        } catch (Exception e) {
            log.error("[讯飞ASR] 解析识别结果data失败: {}", dataJson, e);
        }
        return ""; // 只返回最终结果，中间结果忽略
    }


    public static String getHandshakeUrl(String host, String appId, String secretKey) throws SignatureException, UnsupportedEncodingException {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String baseString = appId + ts;
        String signa = EncryptUtil.HmacSHA1Encrypt(EncryptUtil.MD5(baseString), secretKey);
        String encodedSigna = URLEncoder.encode(signa, "UTF-8");

        // 如果您需要开启其他功能，可以在这里拼接参数, e.g., lang=en
        return String.format("wss://%s?appid=%s&ts=%s&signa=%s", host, appId, ts, encodedSigna);
    }

    private static SSLSocketFactory createSslSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("创建SSL Socket Factory失败", e);
        }
    }
}