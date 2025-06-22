package com.echo.virtual_interview.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 知识库-单表JSON实现
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("knowledge_base_questions")
@Schema(name="KnowledgeBaseQuestions对象", description="知识库-单表JSON实现")
public class KnowledgeBaseQuestions implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "问题唯一ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "问题题干")
    private String questionText;

    @SchemaProperty(name = "参考答案或得分要点（AI评分关键依据）")
    private String answerGuideline;

    @SchemaProperty(name = "难度等级('简单', '中等', '困难')")
    private String difficulty;

    @Schema(description = "关联的知识点/主题（JSON数组格式）",
            example = "[\"计算机网络\", \"TCP/IP\"]")
    private String topics;

    @Schema(description = "适用的岗位（JSON数组格式）",
            example = "[\"后端工程师\", \"网络工程师\"]")
    private String positions;

    @Schema(description = "适用的工作类型（JSON数组格式）",
            example = "[\"校招\", \"社招\"]")
    private String jobTypes;

    @Schema(description = "适用的面试官风格（JSON数组格式）",
            example = "[\"技术型\", \"压力面\"]")
    private String styles;

    @Schema(description = "关联的公司（JSON数组格式）",
            example = "[\"腾讯\", \"阿里巴巴\"]")
    private String companies;

    @SchemaProperty(name = "问题状态('草稿', '审核中', '已发布', '已归档')")
    private String status;

    @SchemaProperty(name = "问题创建者ID")
    private Long creatorId;

    @SchemaProperty(name = "创建时间")
    @TableField(fill = FieldFill.INSERT) // <<--- 添加此注解

    private LocalDateTime createdAt;

    @SchemaProperty(name = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE) // <<--- 添加此注解

    private LocalDateTime updatedAt;


}
