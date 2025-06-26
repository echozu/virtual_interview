package com.echo.virtual_interview.model.entity;

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
 * 存储面试过程中的每一轮对话及初步分析
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("interview_dialogue")
@Schema(name="InterviewDialogue对象", description="存储面试过程中的每一轮对话及初步分析")
public class InterviewDialogue implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "对话记录ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "所属面试会话ID")
    private String sessionId;

    @SchemaProperty(name = "对话顺序（如1、2、3）")
    private Integer sequence;

    @SchemaProperty(name = "ai提问的问题或回答（因为用户可能会问面试官问题）")
    private String aiMessage;

    @SchemaProperty(name = "用户回答的内容或提问的问题")
    private String userMessage;

    @SchemaProperty(name = "消息时间戳（精确到毫秒）")
    private LocalDateTime timestamp;

    @SchemaProperty(name = "该轮对话的分析(如：用户在回答这道题时，包含：1.眼神分析 2.情绪分析 3.回答与正确内容的分析【眼神飘忽不定,答案不尽人意....】)")
    private String turnAnalysis;


}
