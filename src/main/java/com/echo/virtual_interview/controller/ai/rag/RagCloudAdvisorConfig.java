package com.echo.virtual_interview.controller.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG (检索增强生成) 的 Advisor 配置
 */
@Configuration
@Slf4j
public class RagCloudAdvisorConfig {

    /**
     * 配置基于云知识库的检索增强顾问 Bean
     *
     * @param xfyunSparkKnowledgeRetriever Spring Boot 自动注入的自定义文档检索器
     * @return Advisor
     */
    @Bean
    public Advisor RagCloudAdvisor(DocumentRetriever xfyunSparkKnowledgeRetriever) {
        log.info("正在配置基于讯飞知识库的Advisor...");

        // 【最终修正】: 根据源码，Builder中的方法名为 documentRetriever
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(xfyunSparkKnowledgeRetriever)
                .build();
    }
}