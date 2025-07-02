package com.echo.virtual_interview.controller.ai.rag.util;

import com.echo.virtual_interview.controller.ai.rag.XfyunSparkKnowledgeClient;
import com.echo.virtual_interview.controller.ai.rag.XfyunSparkKnowledgeRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 讯飞星火云知识库的 Spring Boot 自动配置类
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(DocumentRetriever.class)
@EnableConfigurationProperties(XfyunSparkKnowledgeProperties.class)
@ConditionalOnProperty(
        prefix = "xunfei.knowledge",
        name = {"app-id", "secret", "knowledge-base-name"}
)
public class XfyunSparkKnowledgeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper xfyunApiObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public XfyunSparkKnowledgeClient xfyunSparkKnowledgeClient(
            XfyunSparkKnowledgeProperties properties,
            ObjectMapper objectMapper) {
        log.info("正在创建讯飞知识库客户端...");
        return new XfyunSparkKnowledgeClient(properties, objectMapper);
    }

    /**
     * 创建 DocumentRetriever 的 Bean
     * 这个 Bean 实现了从讯飞知识库检索文档的核心逻辑
     */
    @Bean
    @ConditionalOnMissingBean
    public DocumentRetriever xfyunSparkKnowledgeRetriever(
            XfyunSparkKnowledgeClient client,
            XfyunSparkKnowledgeProperties properties) {
        log.info("正在创建讯飞知识库 DocumentRetriever...");
        return new XfyunSparkKnowledgeRetriever(client, properties.getKnowledgeBaseName());
    }
}