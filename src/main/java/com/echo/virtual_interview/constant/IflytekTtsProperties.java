package com.echo.virtual_interview.constant;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "xunfei.tts")
public class IflytekTtsProperties {

    /**
     * 请求地址的主机部分
     */
    private String host;

    /**
     * 项目的 AppID
     */
    private String appId;

    /**
     * 项目的 API Key
     */
    private String apiKey;

    /**
     * 项目的 API Secret
     */
    private String apiSecret;
    
    /**
     * 使用的发音人
     */
    private String vcn;
}