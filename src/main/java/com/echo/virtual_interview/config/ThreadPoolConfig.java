package com.echo.virtual_interview.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfig.class);

    private ExecutorService videoAnalysisExecutor;

    /**
     * 创建一个单线程的执行器，用于串行处理视频分析等耗时任务。
     * @return ExecutorService 实例
     */
    @Bean("videoAnalysisExecutor")
    public ExecutorService videoAnalysisExecutor() {
        // 使用一个命名的线程工厂，方便在日志和监控中识别线程
        this.videoAnalysisExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "video-analysis-thread-1"));
        return this.videoAnalysisExecutor;
    }

    /**
     * 使用 @PreDestroy 注解实现优雅关闭。
     * 这是 Spring 推荐的、用于在 Bean 销毁前执行清理操作的标准方式。
     * 当 Spring Boot 应用关闭时，此方法会被自动调用。
     */
    @PreDestroy
    public void shutdownExecutor() {
        if (videoAnalysisExecutor == null || videoAnalysisExecutor.isShutdown()) {
            return;
        }

        log.info("Gracefully shutting down the 'videoAnalysisExecutor'...");

        // 1. 禁止提交新任务
        videoAnalysisExecutor.shutdown();
        try {
            // 2. 等待现有任务在设定的时间内完成
            if (!videoAnalysisExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                // 3. 如果超时仍未完成，强制关闭
                log.warn("'videoAnalysisExecutor' did not terminate in 30 seconds. Forcing shutdown...");
                videoAnalysisExecutor.shutdownNow();

                // 4. 再次等待，以响应被强制取消的任务
                if (!videoAnalysisExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
                    log.error("'videoAnalysisExecutor' did not terminate even after forceful shutdown.");
                }
            }
        } catch (InterruptedException ie) {
            // 5. 如果当前线程在等待时被中断，也尝试强制关闭
            log.warn("Shutdown of 'videoAnalysisExecutor' was interrupted. Forcing shutdown...");
            videoAnalysisExecutor.shutdownNow();
            // 保持中断状态
            Thread.currentThread().interrupt();
        }

        log.info("'videoAnalysisExecutor' has been successfully shut down.");
    }
}