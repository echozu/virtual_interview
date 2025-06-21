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
     * 拦截 Controller 包下所有方法
     */
    @Around("execution(* com.echo.virtual_interview.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        // 创建计时器，用于统计请求耗时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 获取当前请求对象
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 生成唯一的请求 ID，方便日志追踪
        String requestId = UUID.randomUUID().toString();

        // 获取请求路径
        String url = httpServletRequest.getRequestURI();

        // 获取请求参数
        Object[] args = point.getArgs();
        String reqParam = "[" + StringUtils.join(args, ", ") + "]";

        // 打印请求日志：请求 ID，请求路径，请求 IP，请求参数
        log.info("request start，id: {}, path: {}, ip: {}, params: {}", requestId, url,
                httpServletRequest.getRemoteHost(), reqParam);

        // 执行原方法（即实际的 Controller 方法）
        Object result = point.proceed();

        // 停止计时
        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();

        // 打印响应日志：请求 ID，请求耗时
        log.info("request end, id: {}, cost: {}ms", requestId, totalTimeMillis);

        // 返回原方法的返回值
        return result;
    }
}
