package com.echo.virtual_interview.controller.ai.rag;

import com.echo.virtual_interview.controller.ai.rag.util.ApiAuthUtil;
import com.echo.virtual_interview.controller.ai.rag.util.XfyunApiDto;
import com.echo.virtual_interview.controller.ai.rag.util.XfyunSparkKnowledgeProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 讯飞星火云知识库 API 客户端
 */
@Slf4j
public class XfyunSparkKnowledgeClient {

    private static final String API_BASE_URL = "https://chatdoc.xfyun.cn/openapi/v1";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final XfyunSparkKnowledgeProperties properties;

    public XfyunSparkKnowledgeClient(XfyunSparkKnowledgeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public List<String> getFileIdsByKnowledgeBaseName(String knowledgeBaseName) {
        log.info("开始根据知识库名称 '{}' 获取文件ID列表...", knowledgeBaseName);
        String repoId = findRepoIdByName(knowledgeBaseName);
        if (repoId == null) {
            log.error("未找到名称为 '{}' 的知识库。", knowledgeBaseName);
            // 这里抛出异常，让应用启动失败，因为这是关键依赖
            throw new IllegalArgumentException("未找到名称为 '" + knowledgeBaseName + "' 的讯飞知识库");
        }
        log.info("成功找到知识库ID: {}", repoId);
        return listAllFilesByRepoId(repoId);
    }

    public List<XfyunApiDto.SearchResult> vectorSearch(String query, List<String> fileIds) {
        String url = API_BASE_URL + "/vector/search";
        Map<String, Object> payload = new HashMap<>();
        payload.put("fileIds", fileIds);
        payload.put("content", query);
        payload.put("topN", 5);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            RequestBody body = RequestBody.create(jsonPayload, JSON);
            Request request = buildRequest(url).post(body).build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("向量检索失败，HTTP状态码: {}, 响应: {}", response.code(), response.body() != null ? response.body().string() : "null");
                    return Collections.emptyList();
                }

                String responseBody = response.body().string();
                log.debug("向量检索响应: {}", responseBody);
                XfyunApiDto.VectorSearchResponse result = objectMapper.readValue(responseBody, XfyunApiDto.VectorSearchResponse.class);

                if (result.getCode() != 0) {
                    log.error("向量检索API返回错误: code={}, desc={}", result.getCode(), result.getDesc());
                    return Collections.emptyList();
                }
                return result.getData();
            }
        } catch (IOException e) {
            log.error("执行向量检索时发生 I/O 异常", e);
            return Collections.emptyList();
        }
    }

    private String findRepoIdByName(String name) {
        String url = API_BASE_URL + "/repo/list";
        Map<String, Object> payload = Collections.singletonMap("repoName", name);
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            RequestBody body = RequestBody.create(jsonPayload, JSON);
            Request request = buildRequest(url).post(body).build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("查询知识库列表失败, HTTP状态码: {}", response.code());
                    return null;
                }
                String responseBody = response.body().string();
                log.debug("知识库列表API响应: {}", responseBody);

                // 【核心修正】使用修正后的 RepoListResponse.class 进行反序列化
                XfyunApiDto.RepoListResponse result = objectMapper.readValue(responseBody, XfyunApiDto.RepoListResponse.class);

                // 【核心修正】从分页对象中获取 'rows' 列表
                if (result.getCode() == 0 && result.getData() != null && result.getData().getRows() != null) {
                    return result.getData().getRows().stream()
                            .filter(repo -> name.equals(repo.getRepoName())) // 精确匹配名称
                            .map(XfyunApiDto.RepoInfo::getRepoId)
                            .findFirst()
                            .orElse(null);
                }
            }
        } catch (IOException e) {
            log.error("查询知识库列表时发生 I/O 异常", e);
        }
        return null;
    }

    private List<String> listAllFilesByRepoId(String repoId) {
        String url = API_BASE_URL + "/repo/file/list";
        Map<String, Object> payload = new HashMap<>();
        payload.put("repoId", repoId);
        payload.put("pageSize", 100);

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            RequestBody body = RequestBody.create(jsonPayload, JSON);
            Request request = buildRequest(url).post(body).build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.error("查询知识库文件列表失败, HTTP状态码: {}", response.code());
                    return Collections.emptyList();
                }
                String responseBody = response.body().string();
                XfyunApiDto.RepoFileListResponse result = objectMapper.readValue(responseBody, XfyunApiDto.RepoFileListResponse.class);

                if (result.getCode() == 0 && result.getData() != null && result.getData().getRows() != null) {
                    List<String> fileIds = result.getData().getRows().stream()
                            .filter(file -> "vectored".equalsIgnoreCase(file.getFileStatus()))
                            .map(XfyunApiDto.RepoFileInfo::getFileId)
                            .collect(Collectors.toList());
                    log.info("在知识库 '{}' 中找到 {} 个可用的文件ID。", repoId, fileIds.size());
                    return fileIds;
                }
            }
        } catch (IOException e) {
            log.error("查询知识库文件列表时发生 I/O 异常", e);
        }
        return Collections.emptyList();
    }

    private Request.Builder buildRequest(String url) {
        long timestamp = System.currentTimeMillis() / 1000;
        try {
            String signature = ApiAuthUtil.getSignature(properties.getAppId(), properties.getSecret(), timestamp);
            return new Request.Builder()
                    .url(url)
                    .addHeader("appId", properties.getAppId())
                    .addHeader("timestamp", String.valueOf(timestamp))
                    .addHeader("signature", signature);
        } catch (SignatureException e) {
            log.error("生成API签名失败!", e);
            throw new RuntimeException("生成讯飞API签名失败", e);
        }
    }
}