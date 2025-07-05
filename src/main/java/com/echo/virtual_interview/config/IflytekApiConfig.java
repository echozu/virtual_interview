
// =====================================================================================
// Configuration Class (配置类)
// =====================================================================================
package com.echo.virtual_interview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "xunfei.face-score")
public class IflytekApiConfig {
    private String appid;
    private String apikey;

    // Getters and Setters
    public String getAppid() { return appid; }
    public void setAppid(String appid) { this.appid = appid; }
    public String getApikey() { return apikey; }
    public void setApikey(String apikey) { this.apikey = apikey; }

    // 将RestTemplate注册为Bean，方便在项目中任何地方注入使用
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}