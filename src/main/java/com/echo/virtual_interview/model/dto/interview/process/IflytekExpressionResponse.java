package com.echo.virtual_interview.model.dto.interview.process;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 顶层响应对象, 用于接收讯飞表情识别API的返回结果。
 * 以便在API返回未知字段时程序不会因解析失败而崩溃。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IflytekExpressionResponse(
        int code,
        String desc,
        String sid,
        ResponseData data
) {
    /**
     * 业务数据
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseData(
            @JsonProperty("fileList") List<FileResult> fileList,
            @JsonProperty("reviewCount") int reviewCount,
            List<Integer> statistic
    ) {}

    /**
     * 单个文件的分析结果
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FileResult(
            int label,
            String name,
            double rate,
            boolean review
    ) {}
}