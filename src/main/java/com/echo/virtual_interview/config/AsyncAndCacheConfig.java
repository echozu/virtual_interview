package com.echo.virtual_interview.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync      // 开启Spring的异步方法执行功能
@EnableCaching    // 开启Spring的缓存功能
public class AsyncAndCacheConfig {

    /**
     * 自定义一个线程池，用于执行@Async任务。
     * 这样做可以与Web服务器的线程池（如Tomcat）隔离，避免长时间任务耗尽Web线程。
     * @return Executor
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);      // 核心线程数
        executor.setMaxPoolSize(10);      // 最大线程数
        executor.setQueueCapacity(25);    // 任务队列容量
        executor.setThreadNamePrefix("AsyncTask-"); // 线程名前缀
        executor.initialize();
        return executor;
    }
}
