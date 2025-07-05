package com.echo.virtual_interview.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 存储面试过程中每30秒一次的详细行为分析数据块(V2-简化版)
 * </p>
 *
 * @author echo
 * @since 2025-07-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("interview_analysis_chunk")
@Schema(name="InterviewAnalysisChunk对象", description="存储面试过程中每30秒一次的详细行为分析数据块(V2-简化版)")
public class InterviewAnalysisChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    @SchemaProperty(name = "所属面试会话ID，关联 interview_sessions.id")
    private String sessionId;

    @SchemaProperty(name = "关联的对话记录ID，用于标明此时属于哪一轮问答")
    private String dialogueId;

    @SchemaProperty(name = "该分析片段在面试中的开始时间（秒）")
    private Integer startTimeSeconds;

    @SchemaProperty(name = "该分析片段在面试中的结束时间（秒）")
    private Integer endTimeSeconds;

    @SchemaProperty(name = "Python服务返回的该时间段的完整JSON报告")
    private String pythonAnalysisReport;

    @SchemaProperty(name = "讯飞API返回的表情分析结果JSON报告")
    private String iflytekExpressionReport;

    @SchemaProperty(name = "经由ai生成的完整反馈JSON报告")
    private String aiFeedbackReport;

    @SchemaProperty(name = "记录创建时间")
    private LocalDateTime createdAt;

    @SchemaProperty(name = "记录更新时间")
    private LocalDateTime updatedAt;


}
