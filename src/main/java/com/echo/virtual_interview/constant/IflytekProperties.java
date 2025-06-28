package com.echo.virtual_interview.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "xunfei.rtasr")
public class IflytekProperties {
    private String appid;
    private String secretKey;
    private String host;
}