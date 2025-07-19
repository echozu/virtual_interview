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
 * 存储已生成的完整面试分析报告
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("interview_generated_reports")
@Schema(name="InterviewGeneratedReports对象", description="存储已生成的完整面试分析报告")
public class InterviewGeneratedReports implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "报告的自增主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "对应的面试会话ID，逻辑外键")
    private String sessionId;

    @SchemaProperty(name = "所属用户ID，用于快速查询")
    private Long userId;

    @SchemaProperty(name = "完整的报告DTO序列化后的JSON数据")
    private String reportData;

    @SchemaProperty(name = "记录创建时间")
    private LocalDateTime createdAt;

    @SchemaProperty(name = "记录更新时间")
    private LocalDateTime updatedAt;


}
