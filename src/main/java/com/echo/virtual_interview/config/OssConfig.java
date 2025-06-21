package com.echo.virtual_interview.config;



import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类，用于创建ossUtil对象
 */
@Configuration
public class OssConfig {

    @Value("${echo.oss.endpoint}")
    private String endpoint;

    @Value("${echo.oss.access-key-id}")
    private String accessKeyId;

    @Value("${echo.oss.access-key-secret}")
    private String accessKeySecret;

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
