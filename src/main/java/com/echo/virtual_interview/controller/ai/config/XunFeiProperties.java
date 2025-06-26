package com.echo.virtual_interview.controller.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "xunfei.api")
@Data
public class XunFeiProperties {
    private String key;
    private String url;
    private String model;
}
