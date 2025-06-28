package com.echo.virtual_interview.aop;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求响应日志 AOP 拦截器
 *
 * 作用：拦截 Controller 层所有接口请求，记录请求路径、参数、IP、耗时等日志信息
 */
@Aspect // 表示这是一个切面类
@Component // 注册为 Spring Bean
@Slf4j // 引入日志功能
public class LogInterceptor {

    /**
     * 拦截 Controller 包下所有方法（包括 REST 接口和 STOMP 消息处理器）
     */
    @Around("execution(* com.echo.virtual_interview.controller..*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        // 创建计时器，用于统计请求耗时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String requestId = UUID.randomUUID().toString(); // 请求唯一ID
        String url = "unknown"; // 请求路径默认值
        String ip = "unknown";  // IP 默认值

        // 是否 HTTP 请求标识
        boolean isHttpRequest = true;

        try {
            // 如果当前线程绑定了 HTTP 请求，则记录请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            url = request.getRequestURI();
            ip = request.getRemoteHost();
        } catch (IllegalStateException e) {
            // 当前不是 HTTP 请求，如 STOMP/WebSocket 消息，不处理 request 信息
            isHttpRequest = false;
            log.debug("非 HTTP 请求，跳过 request 信息记录");
        }

        // 获取请求参数（WebSocket 下仍然能获取）
        Object[] args = point.getArgs();
        String reqParam = "[" + StringUtils.join(args, ", ") + "]";

        // 打印请求日志
//        log.info("request start，id: {}, isHttp: {}, path: {}, ip: {}, params: {}",
//                requestId, isHttpRequest, url, ip, reqParam);

        // 执行目标方法
        Object result = point.proceed();

        // 停止计时
        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();

        // 打印响应日志
//        log.info("request end, id: {}, cost: {}ms", requestId, totalTimeMillis);

        return result;
    }
}
