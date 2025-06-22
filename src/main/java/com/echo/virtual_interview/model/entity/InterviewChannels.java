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
 * 存储面试的配置模板，即“频道”
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("interview_channels")
@Schema(name="InterviewChannels对象", description="存储面试的配置模板，即“频道”")
public class InterviewChannels implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "频道唯一ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "创建者ID（NULL表示为官方公开频道）")
    private Long creatorId;

    @SchemaProperty(name = "频道标题")
    private String title;

    @SchemaProperty(name = "频道描述")
    private String description;

    @SchemaProperty(name = "专业方向")
    private String major;

    @SchemaProperty(name = "希望面试的知识点（JSON数组）")
    private String topics;

    @SchemaProperty(name = "工作类型('校招','社招','实习','兼职','不限')")
    private String jobType;

    @SchemaProperty(name = "应聘岗位(如后端）")
    private String targetPosition;

    @SchemaProperty(name = "面试官风格('温和','严肃','技术型','压力面','行为面','随机','自定义')")
    private String interviewerStyle;

    @SchemaProperty(name = "应聘公司")
    private String targetCompany;

    @SchemaProperty(name = "面试形式('一对一', '群面', '多对一')")
    private String interviewMode;

    @SchemaProperty(name = "预计面试时长（分钟）")
    private Integer estimatedDuration;

    @SchemaProperty(name = "可见性('公开','审核中‘, '私有','已拒绝')-用户创建频道时为私有，选择公开则通过管理员审核，审核完为公开状态")
    private String visibility;

    @SchemaProperty(name = "是否被删除（软删除）")
    private Boolean isDeleted;

    @SchemaProperty(name = "创建时间")
    @TableField(fill = FieldFill.INSERT) // <<--- 自动插入时间

    private LocalDateTime createdAt;

    @SchemaProperty(name = "频道封面URL")
    private String imageUrl;

    @SchemaProperty(name = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE) // <<--- 添加此注解
    private LocalDateTime updatedAt;

    @SchemaProperty(name = "频道使用次数-表示受欢迎程度，可以根据这个排序")
    private Integer usageCount;


}
