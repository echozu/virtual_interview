package com.echo.virtual_interview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class VirtualInterviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(VirtualInterviewApplication.class, args);
        System.out.println("=================================");
        System.out.println("🏛️ 虚拟面试平台后端启动成功!");
        System.out.println("📖 API文档地址: http://localhost:9527/api/swagger-ui.html");
        System.out.println("🔗 API接口地址: http://localhost:9527/api/api-docs");
        System.out.println("=================================");
    }

}
