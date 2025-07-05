package com.echo.virtual_interview.config; // 请确保包名正确

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Spring MVC 全局配置
 * 通过实现 WebMvcConfigurer 来强制注入我们自定义的JSON消息转换器，
 * 以确保LocalDateTime等时间类型能被正确序列化，解决多JSON库冲突问题。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 1. 创建一个我们自己配置的ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // 2. 创建Java时间模块，并为其定义序列化和反序列化的格式
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        String format = "yyyy-MM-dd HH:mm:ss";
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(format)));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(format)));

        // 3. 将时间模块注册到ObjectMapper中
        objectMapper.registerModule(javaTimeModule);

        // 4. 创建一个使用我们自定义ObjectMapper的MappingJackson2HttpMessageConverter
        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter(objectMapper);

        // 5. **关键一步**: 将我们自定义的转换器添加到转换器列表的第一个位置，
        // 这样它就会被优先使用，覆盖掉其他任何默认的或冲突的转换器。
        converters.add(0, jackson2HttpMessageConverter);
    }
}