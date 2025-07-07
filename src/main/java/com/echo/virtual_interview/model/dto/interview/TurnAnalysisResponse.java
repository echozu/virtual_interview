package com.echo.virtual_interview.model.dto.interview;

/**
 * 用于接收AI单轮分析的结构化响应 DTO (Data Transfer Object)
 * 使用 record 关键字可以简洁地定义一个不可变的数据类。
 * @param turnScore      AI对本轮对话的评分 (例如 1-10)
 * @param turnSuggestion 对候选人的具体建议
 * @param turnAnalysis   对本轮对话的详细分析
 */
public record TurnAnalysisResponse(
    // 为了更好地进行数据处理和排序，分数建议使用数值类型
    Integer turnScore, 
    String turnSuggestion, 
    String turnAnalysis
) {}