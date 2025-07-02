package com.echo.virtual_interview.controller.ai.rag.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 讯飞星火云知识库统一API工具类
 * <p>
 * 该类封装了讯飞知识库服务的所有常用HTTP和WebSocket接口，包括文档操作和知识库操作。
 * 使用时，只需创建本类的一个实例，即可调用所有相关方法。
 *
 * @version 1.0
 * @see <a href="https://www.xfyun.cn/doc/spark/ChatDoc-API.html">讯飞星火知识库API文档</a>
 */
public class XfyunKnowledgeHelper {
    @Value("xunfei.knowledge.app-id")
    private final String appId;
    @Value("xunfei.knowledge.secret")
    private final String secret;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "https://chatdoc.xfyun.cn/openapi/v1";
    private static final String CHAT_URL = "wss://chatdoc.xfyun.cn/openapi/chat";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /**
     * 构造函数
     *
     * @param appId  在讯飞开放平台申请的应用ID
     * @param secret 在讯飞开放平台申请的应用密钥
     */
    public XfyunKnowledgeHelper(String appId, String secret) {
        this.appId = appId;
        this.secret = secret;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(100, 5, TimeUnit.MINUTES))
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    // =====================================================================================
    // ============================ 1. 基础功能 (文档上传、状态、问答) ============================
    // =====================================================================================

    /**
     * 1.1. 上传文档
     * 上传知识库文档数据，目前支持 doc/docx、pdf、md、txt 格式。
     *
     * @param file 要上传的文件对象
     * @return 上传结果的响应对象
     * @throws IOException IO异常
     */
    public UploadResp uploadFile(File file) throws IOException {
        String url = BASE_URL + "/file/upload";
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse("multipart/form-data")))
                .addFormDataPart("fileType", "wiki")
                .build();
        Request request = buildPostRequest(url, body);
        return execute(request, UploadResp.class);
    }

    /**
     * 1.2. 查询文档状态
     *
     * @param fileIds 一个或多个文档ID，用英文逗号分隔
     * @return 文档状态的响应对象
     * @throws IOException IO异常
     */
    public StatusResp queryFileStatus(String fileIds) throws IOException {
        String url = BASE_URL + "/file/status";
        RequestBody body = new FormBody.Builder()
                .add("fileIds", fileIds)
                .build();
        Request request = buildPostRequest(url, body);
        return execute(request, StatusResp.class);
    }

    /**
     * 1.3. 文档问答 (流式返回)
     * 注意：此方法会阻塞线程，直到收到完整的回答或发生错误。
     *
     * @param fileIds  要进行问答的文档ID列表
     * @param question 用户提出的问题
     * @return 拼接好的完整回答字符串
     * @throws InterruptedException 线程中断异常
     */
    public String chatWithFile(List<String> fileIds, String question) throws InterruptedException {
        long ts = System.currentTimeMillis() / 1000;
        String signature = ApiAuthUtil.getSignature(this.appId, this.secret, ts);
        String requestUrl = CHAT_URL + "?appId=" + this.appId + "&timestamp=" + ts + "&signature=" + signature;

        ChatRequest chatRequest = ChatRequest.builder()
                .fileIds(fileIds)
                .messages(Collections.singletonList(new ChatMessage("user", question)))
                .build();

        final StringBuilder answerBuffer = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);

        Request wsRequest = new Request.Builder().url(requestUrl).build();
        WebSocket webSocket = httpClient.newWebSocket(wsRequest, new WebSocketListener() {
            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                try {
                    // 使用 Jackson 解析 JSON
                    ResponseMsg responseMsg = objectMapper.readValue(text, ResponseMsg.class);
                    if (responseMsg.getCode() == 0) {
                        String content = responseMsg.getContent();
                        if (content != null) {
                            answerBuffer.append(content);
                        }
                        // status=2 表示最后一个结果
                        if (Objects.equals(responseMsg.getStatus(), 2)) {
                            latch.countDown();
                        }
                    } else {
                        System.err.println("WebSocket返回错误: " + text);
                        latch.countDown();
                    }
                } catch (JsonProcessingException e) {
                    System.err.println("WebSocket消息解析失败: " + text);
                    e.printStackTrace();
                    latch.countDown();
                }
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                System.err.println("WebSocket连接失败: " + t.getMessage());
                latch.countDown();
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                webSocket.close(1000, null);
                latch.countDown();
            }
        });

        try {
            webSocket.send(objectMapper.writeValueAsString(chatRequest));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("构建请求失败", e);
        }

        // 等待WebSocket通信完成
        latch.await(60, TimeUnit.SECONDS);
        return answerBuffer.toString();
    }


    // =====================================================================================
    // ============================ 2. 高级功能 (文档总结、操作) ==============================
    // =====================================================================================

    /**
     * 2.1. 发起文档总结任务
     *
     * @param fileId 文件ID
     * @return 响应对象
     * @throws IOException IO异常
     */
    public BaseResponse<Object> startSummary(String fileId) throws IOException {
        String url = BASE_URL + "/file/summary/start";
        RequestBody body = new FormBody.Builder().add("fileId", fileId).build();
        Request request = buildPostRequest(url, body);
        return execute(request, BaseResponse.class);
    }

    /**
     * 2.2. 获取文档总结信息
     *
     * @param fileId 文件ID
     * @return 包含总结状态和内容的响应对象
     * @throws IOException IO异常
     */
    public SummaryResp querySummary(String fileId) throws IOException {
        String url = BASE_URL + "/file/summary/query";
        RequestBody body = new FormBody.Builder().add("fileId", fileId).build();
        Request request = buildPostRequest(url, body);
        return execute(request, SummaryResp.class);
    }

    /**
     * 2.3. 文档向量化
     *
     * @param fileIds 一个或多个文件ID，用英文逗号分隔
     * @return 响应对象
     * @throws IOException IO异常
     */
    public BaseResponse<Object> embedFile(String fileIds) throws IOException {
        String url = BASE_URL + "/file/embedding";
        RequestBody body = new FormBody.Builder().add("fileIds", fileIds).build();
        Request request = buildPostRequest(url, body);
        return execute(request, BaseResponse.class);
    }

    // =====================================================================================
    // ============================ 3. 知识库操作 =========================================
    // =====================================================================================

    /**
     * 3.1. 创建知识库
     *
     * @param repoName 知识库名称 (唯一)
     * @param repoDesc 知识库简介 (可选)
     * @param repoTags 知识库标签, 逗号分隔 (可选)
     * @return 包含新知识库ID的响应对象
     * @throws IOException IO异常
     */
    public BaseResponse<String> createKnowledgeBase(String repoName, String repoDesc, String repoTags) throws IOException {
        String url = BASE_URL + "/repo/create";
        Map<String, String> payload = new HashMap<>();
        payload.put("repoName", repoName);
        if (repoDesc != null) payload.put("repoDesc", repoDesc);
        if (repoTags != null) payload.put("repoTags", repoTags);

        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), JSON_MEDIA_TYPE);
        Request request = buildPostRequest(url, body);
        return execute(request, BaseResponse.class);
    }

    /**
     * 3.2. 向知识库添加文件
     *
     * @param repoId  知识库ID
     * @param fileIds 要添加的文件ID列表
     * @return 响应对象
     * @throws IOException IO异常
     */
    public BaseResponse<Object> addFilesToKnowledgeBase(String repoId, List<String> fileIds) throws IOException {
        String url = BASE_URL + "/repo/file/add";
        Map<String, Object> payload = new HashMap<>();
        payload.put("repoId", repoId);
        payload.put("fileIds", fileIds);

        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), JSON_MEDIA_TYPE);
        Request request = buildPostRequest(url, body);
        return execute(request, BaseResponse.class);
    }

    /**
     * 3.3. 查询知识库列表
     *
     * @param repoName 知识库名称，用于模糊查询 (可选)
     * @return 响应对象
     * @throws IOException IO异常
     */
    public BaseResponse<Object> listKnowledgeBases(String repoName) throws IOException {
        String url = BASE_URL + "/repo/list";
        Map<String, String> payload = new HashMap<>();
        if (repoName != null) payload.put("repoName", repoName);

        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(payload), JSON_MEDIA_TYPE);
        Request request = buildPostRequest(url, body);
        return execute(request, BaseResponse.class);
    }

    /**
     * 3.4. 删除知识库
     *
     * @param repoId 要删除的知识库ID
     * @return 响应对象
     * @throws IOException IO异常
     */
    public BaseResponse<Object> deleteKnowledgeBase(String repoId) throws IOException {
        String url = BASE_URL + "/repo/del";
        RequestBody body = new FormBody.Builder().add("repoId", repoId).build();
        Request request = buildPostRequest(url, body);
        return execute(request, BaseResponse.class);
    }


    // =====================================================================================
    // ============================ 私有辅助方法 ===========================================
    // =====================================================================================

    private Request buildPostRequest(String url, RequestBody body) {
        long ts = System.currentTimeMillis() / 1000;
        String signature = ApiAuthUtil.getSignature(this.appId, this.secret, ts);
        return new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("appId", this.appId)
                .addHeader("timestamp", String.valueOf(ts))
                .addHeader("signature", signature)
                .build();
    }

    private <T> T execute(Request request, Class<T> responseClass) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: " + response.code() + " " + response.message());
            }
            String responseBody = Objects.requireNonNull(response.body()).string();
            return objectMapper.readValue(responseBody, responseClass);
        }
    }


    // =====================================================================================
    // ============================ 内部静态工具类和DTOs ====================================
    // =====================================================================================

    /**
     * 内部静态类：API鉴权签名生成工具
     */
    private static class ApiAuthUtil {
        private static final char[] MD5_TABLE = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        public static String getSignature(String appId, String secret, long ts) {
            try {
                String auth = md5(appId + ts);
                return hmacSHA1Encrypt(auth, secret);
            } catch (Exception e) {
                throw new RuntimeException("生成签名失败", e);
            }
        }

        private static String hmacSHA1Encrypt(String encryptText, String encryptKey) throws SignatureException {
            try {
                byte[] data = encryptKey.getBytes(StandardCharsets.UTF_8);
                SecretKeySpec secretKey = new SecretKeySpec(data, "HmacSHA1");
                Mac mac = Mac.getInstance("HmacSHA1");
                mac.init(secretKey);
                byte[] text = encryptText.getBytes(StandardCharsets.UTF_8);
                byte[] rawHmac = mac.doFinal(text);
                return Base64.getEncoder().encodeToString(rawHmac);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                throw new SignatureException("HmacSHA1加密失败: " + e.getMessage(), e);
            }
        }

        private static String md5(String cipherText) throws NoSuchAlgorithmException {
            byte[] data = cipherText.getBytes();
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(data);
            byte[] md = mdInst.digest();
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = MD5_TABLE[byte0 >>> 4 & 0xf];
                str[k++] = MD5_TABLE[byte0 & 0xf];
            }
            return new String(str);
        }
    }


    // --- DTO (Data Transfer Objects) ---

    @Data
    public static class BaseResponse<T> {
        private boolean flag;
        private int code;
        private String desc;
        private String sid;
        private T data;
    }

    @Getter
    @Setter
    public static class UploadResp extends BaseResponse<UploadResp.Datas> {

        /**
         * 【核心修正】: 添加此注解以忽略未在类中定义的任何JSON字段 (如此处的 letterNum)
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        @Data
        public static class Datas {
            private String fileId;
            private String parseType;
            private int quantity;
        }
    }


    @Getter
    @Setter
    public static class StatusResp extends BaseResponse<List<StatusResp.Datas>> {
        @JsonIgnoreProperties(ignoreUnknown = true)
        @Data
        public static class Datas {
            private String fileId;
            private String fileStatus;
        }
    }

    @Getter
    @Setter
    public static class SummaryResp extends BaseResponse<SummaryResp.Datas> {
        @Data
        public static class Datas {
            private String summaryStatus;
            private String summary;
        }
    }
    
    // --- WebSocket DTOs ---

    @Data
    public static class ResponseMsg {
        private int code;
        private String content;
        private String sid;
        private Integer status;
    }

    @Getter
    @Setter
    @Builder
    public static class ChatRequest {
        private List<String> fileIds;
        private List<ChatMessage> messages;
        private Integer topN;
        private ChatExtends chatExtends;
    }

    @Getter
    @Setter
    public static class ChatMessage {
        private String role;
        private String content;
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Getter
    @Setter
    @Builder
    public static class ChatExtends {
        private String wikiPromptTpl;
        private Boolean spark;
    }

    // =====================================================================================
    // ============================ 使用示例 (main方法) ====================================
    // =====================================================================================

    public static void main(String[] args) {
        // 1. 初始化工具类
        // 请替换为在讯飞开放平台申请的真实appid和secret
        String appId = "bbbfe203";
        String secret = "***REMOVED***";
        XfyunKnowledgeHelper helper = new XfyunKnowledgeHelper(appId, secret);

        try {
/*
            // --- 示例1: 上传文件并查询状态 ---
            System.out.println("--- 示例1: 上传文件并查询状态 ---");
            File testFile = new File("src/main/resources/test.txt"); // 请确保该文件存在
            if (!testFile.exists()) {
                System.err.println("示例文件 'src/main/resources/test.txt' 不存在，请创建后再运行。");
                return;
            }
            UploadResp uploadResp = helper.uploadFile(testFile);
            if (uploadResp.getCode() == 0) {
                String fileId = uploadResp.getData().getFileId();
                System.out.println("文件上传成功，File ID: " + fileId);

                // 轮询查询文件状态，直到其可用于问答
                while (true) {
                    StatusResp statusResp = helper.queryFileStatus(fileId);
                    String status = statusResp.getData().get(0).getFileStatus();
                    System.out.println("当前文件状态: " + status);
                    if ("vectored".equalsIgnoreCase(status)) {
                        System.out.println("文件已准备好，可以进行问答。");
                        
                        // --- 示例2: 进行文档问答 ---
                        System.out.println("\n--- 示例2: 进行文档问答 ---");
                        String question = "这篇文档讲了什么？";
                        System.out.println("提问: " + question);
                        String answer = helper.chatWithFile(Collections.singletonList(fileId), question);
                        System.out.println("回答: " + answer);
                        break;
                    } else if ("failed".equalsIgnoreCase(status)) {
                        System.err.println("文件处理失败。");
                        break;
                    }
                    Thread.sleep(5000); // 每5秒查询一次
                }
            } else {
                System.err.println("文件上传失败: " + uploadResp.getDesc());
            }
*/

            // --- 示例3: 创建知识库 ---
            System.out.println("\n--- 示例3: 创建知识库 ---");
            String repoName = "我的测试知识库_" + System.currentTimeMillis();
            System.out.println(repoName);
            BaseResponse<String> createRepoResp = helper.createKnowledgeBase(repoName, "这是一个测试知识库", "测试,Java");
            if (createRepoResp.getCode() == 0) {
                String repoId = createRepoResp.getData();
                System.out.println("知识库创建成功，Repo ID: " + repoId);
            } else {
                System.err.println("知识库创建失败: " + createRepoResp.getDesc());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
