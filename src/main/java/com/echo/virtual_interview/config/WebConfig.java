package com.echo.virtual_interview.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 
 * @author echo
 * @description 配置拦截器和其他Web相关设置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**") // 拦截所有API请求
                .excludePathPatterns(
                        "/api/user/register",    // 注册接口
                        "/api/user/login",       // 登录接口
                        "/api/account/sms",         // 验证码接口
                        "/swagger-ui/**",           // Swagger UI
                        "/api-docs/**",             // API文档
                        "/v3/api-docs/**",           // OpenAPI文档
                        "/ws/**" // 放行 WebSocket 请求路径

                );
    }
}
