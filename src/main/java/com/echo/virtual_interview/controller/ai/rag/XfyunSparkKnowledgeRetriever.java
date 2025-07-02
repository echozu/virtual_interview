package com.echo.virtual_interview.controller.ai.rag;

import com.echo.virtual_interview.controller.ai.rag.util.XfyunApiDto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query; // (1) 确保导入正确的 Query 类
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 讯飞星火云知识库的文档检索器
 * 实现了 Spring AI 的 DocumentRetriever 接口 (已修正)
 */
@Slf4j
public class XfyunSparkKnowledgeRetriever implements DocumentRetriever {

    private final XfyunSparkKnowledgeClient client;
    private final String knowledgeBaseName;

    /**
     * 缓存从知识库名称解析出的文件ID列表，避免每次检索都重新查询
     */
    private List<String> cachedFileIds;

    public XfyunSparkKnowledgeRetriever(XfyunSparkKnowledgeClient client, String knowledgeBaseName) {
        this.client = client;
        this.knowledgeBaseName = knowledgeBaseName;
    }

    /**
     * Spring Bean 初始化后执行此方法
     * 核心作用是预先加载并缓存知识库对应的所有文件ID
     */
    @PostConstruct
    public void initialize() {
        log.info("正在初始化讯飞知识库检索器，目标知识库: '{}'", knowledgeBaseName);
        this.cachedFileIds = client.getFileIdsByKnowledgeBaseName(knowledgeBaseName);
        if (CollectionUtils.isEmpty(this.cachedFileIds)) {
            log.warn("警告：未能从知识库 '{}' 中获取到任何可用的文件ID。检索功能可能无法正常工作。", knowledgeBaseName);
        } else {
            log.info("成功缓存了 {} 个文件ID，用于知识库 '{}' 的检索。", this.cachedFileIds.size(), knowledgeBaseName);
        }
    }

    /**
     * (2) 修正了 retrieve 方法的签名
     * - 参数类型从 Request 变更为 Query
     * - 变量名从 request 变更为 query
     * - 通过 query.getQuery() 获取查询字符串
     */
    @Override
    public List<Document> retrieve(Query query) {
        // 【最终修正】: 从 Query record 中获取查询字符串的正确方法是调用 text()
        String queryString = query.text();
        log.info("接收到检索请求: '{}'", queryString);

        if (CollectionUtils.isEmpty(cachedFileIds)) {
            log.error("无法执行检索，因为知识库 '{}' 中没有可用的文件ID。", knowledgeBaseName);
            return Collections.emptyList();
        }

        // 调用客户端进行向量检索
        List<XfyunApiDto.SearchResult> searchResults = client.vectorSearch(queryString, this.cachedFileIds);

        if (CollectionUtils.isEmpty(searchResults)) {
            log.warn("对于查询 '{}'，未能从知识库中检索到任何相关内容。", queryString);
            return Collections.emptyList();
        }

        // 将讯飞的检索结果转换为 Spring AI 的 Document 对象
        return searchResults.stream()
                .map(this::toSpringAiDocument)
                .collect(Collectors.toList());
    }

    /**
     * 将讯飞 API 的检索结果转换为 Spring AI 的 Document 格式
     * @param result 讯飞的单条检索结果
     * @return Spring AI Document 对象
     */
    private Document toSpringAiDocument(XfyunApiDto.SearchResult result) {
        // 将检索结果的元信息放入 metadata 中，便于后续追踪
        Map<String, Object> metadata = Map.of(
                "fileId", result.getFileId(),
                "score", result.getScore(),
                "chunkIndex", result.getChunkIndex(),
                "knowledgeBaseName", this.knowledgeBaseName
        );
        // Document 的 id 可以自定义，这里用 fileId 和 chunkIndex 组合
        String docId = String.format("%s-%d", result.getFileId(), result.getChunkIndex());
        return new Document(docId, result.getContent(), metadata);
    }
}