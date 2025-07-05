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
 * 记录每一次具体的面试实例
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("interview_sessions")
@Schema(name="InterviewSessions对象", description="记录每一次具体的面试实例")
public class InterviewSessions implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "会话唯一ID")
    @TableId(value = "id")
    private String id;

    @SchemaProperty(name = "参与面试的用户ID")
    private Long userId;

    @SchemaProperty(name = "使用的面试频道ID（可为空，表示快速开始）")
    private Long channelId;

    @SchemaProperty(name = "本次面试使用的简历ID")
    private Long resumeId;

    @SchemaProperty(name = "面试状态('准备中', '进行中', '已完成', '已取消')")
    private String status;

    @SchemaProperty(name = "面试开始时间")
    private LocalDateTime startedAt;

    @SchemaProperty(name = "面试结束时间")
    private LocalDateTime endedAt;

    @SchemaProperty(name = "是否开启视频")
    private Boolean videoEnabled;

    @SchemaProperty(name = "是否开启麦克风")
    private Boolean micEnabled;

    @SchemaProperty(name = "反馈模式('实时反馈', '模拟面试')")
    private String feedbackMode;

    @SchemaProperty(name = "面试综合得分")
    private BigDecimal overallScore;

    @SchemaProperty(name = "记录创建时间")
    private LocalDateTime createdAt;

    @SchemaProperty(name = "记录更新时间")
    private LocalDateTime updatedAt;

}
