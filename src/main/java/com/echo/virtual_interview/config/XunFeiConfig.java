package com.echo.virtual_interview.config;

import com.echo.virtual_interview.constant.XunFeiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(XunFeiProperties.class)
public class XunFeiConfig {
}
