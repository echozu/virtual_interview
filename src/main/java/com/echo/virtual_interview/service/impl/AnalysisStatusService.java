package com.echo.virtual_interview.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalysisStatusService {

    // 使用 ConcurrentHashMap 作为内存缓存来存储任务状态和结果
    // Key: analysisId, Value: 任务结果的封装
    private final Map<String, AnalysisResult> analysisCache = new ConcurrentHashMap<>();

    // 任务结果的封装类
    public record AnalysisResult(String status, Object data) {}

    // 当Python任务创建时，Java可以预先创建一个 PENDING 状态
    public void createTask(String analysisId) {
        analysisCache.put(analysisId, new AnalysisResult("PENDING", "任务正在处理中..."));
    }
    
    // 当Java后端处理完Python发来的数据后，更新任务状态和结果
    public void updateTaskResult(String analysisId, String status, Object data) {
        analysisCache.put(analysisId, new AnalysisResult(status, data));
    }

    // 供轮询接口调用，获取任务状态
    public AnalysisResult getTaskResult(String analysisId) {
        return analysisCache.get(analysisId);
    }
}