package com.echo.virtual_interview.model.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 存储详细的面试后分析报告
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("analysis_reports")
@Schema(name="AnalysisReports对象", description="存储详细的面试后分析报告")
public class AnalysisReports implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "报告唯一ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "对应的面试会话ID")
    private String sessionId;

    @SchemaProperty(name = "报告生成时间")
    private LocalDateTime generatedAt;

    @SchemaProperty(name = "总体表现概述")
    private String overallSummary;

    @SchemaProperty(name = "核心能力-专业知识水平得分")
    private BigDecimal scoreProfessionalKnowledge;

    @SchemaProperty(name = "核心能力-技能匹配度得分")
    private BigDecimal scoreSkillMatch;

    @SchemaProperty(name = "核心能力-语言表达能力得分")
    private BigDecimal scoreVerbalExpression;

    @SchemaProperty(name = "核心能力-逻辑思维能力得分")
    private BigDecimal scoreLogicalThinking;

    @SchemaProperty(name = "核心能力-创新能力得分")
    private BigDecimal scoreInnovation;

    @SchemaProperty(name = "核心能力-应变抗压能力得分")
    private BigDecimal scoreStressResistance;

    @SchemaProperty(name = "面试行为表现分析总结")
    private String behavioralAnalysis;

    @SchemaProperty(name = "面试行为表现改进建议")
    private String behavioralSuggestions;

    @Schema(description = "紧张度曲线数据（JSON数组格式）", example = "[{\"time\": \"01:30\", \"value\": 20}]")
    private String tensionCurveData;


    @Schema(description = "综合建议文本")
    private String overallSuggestions;

    @Schema(description = "口头禅/填充词使用频率（JSON对象格式）", example = "{\"嗯\": 10, \"那个\": 5}")
    private String fillerWordUsage;

    @SchemaProperty(name = "眼神交流（注视摄像头）分析得分，【以问题为界限，如：问题1-眼神飘忽不定：0.3 问题2：正视镜头:0.8】")
    private String eyeContactPercentage;


}
