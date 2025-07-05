package com.echo.virtual_interview.model.dto.interview.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map; /**
 * 紧张度分析
 * Nervousness analysis.
 */
@Data
public class NervousnessAnalysis {

    /**
     * 基于规则模型计算出的综合紧张度评分
     * Overall nervousness score calculated based on a rule-based model.
     */
    @JsonProperty("overall_score")
    private int overallScore;

    /**
     * 将分数映射成的可读等级，如 "高度紧张", "状态放松"
     * Readable level mapped from the score, e.g., "Highly Nervous", "Relaxed".
     */
    private String level;

    /**
     * 构成评分的各个行为指标的详细量化分析
     * Detailed quantitative analysis of various behavioral indicators that make up the score.
     */
    private Map<String, NervousnessComponent> components;
}
