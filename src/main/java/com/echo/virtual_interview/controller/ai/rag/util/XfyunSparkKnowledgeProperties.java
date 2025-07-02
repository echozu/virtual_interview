package com.echo.virtual_interview.controller.ai.rag.util;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 讯飞星火云知识库的配置属性类
 * 用于从 application.properties 文件中读取以 'spring.ai.xfyun.knowledge' 为前缀的配置
 */
@Data
@ConfigurationProperties(prefix = "xunfei.knowledge")
public class XfyunSparkKnowledgeProperties {

    /**
     * 讯飞开放平台应用 AppId
     */
    private String appId;

    /**
     * 讯飞开放平台应用 Secret
     */
    private String secret;

    /**
     * 要使用的知识库的名称
     */
    private String knowledgeBaseName;
}