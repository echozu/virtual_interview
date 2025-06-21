package com.echo.virtual_interview.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 简历的项目经历（可多个）
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("resume_projects")
@Schema(name="ResumeProjects对象", description="简历的项目经历（可多个）")
public class ResumeProjects implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "项目经历唯一ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "关联的简历ID")
    private Long resumeId;

    @SchemaProperty(name = "项目名称")
    private String projectName;

    @SchemaProperty(name = "担任的职责/角色")
    private String role;

    @SchemaProperty(name = "开始日期")
    private LocalDate startDate;

    @SchemaProperty(name = "结束日期")
    private LocalDate endDate;

    @SchemaProperty(name = "是否至今")
    private Boolean isCurrent;

    @SchemaProperty(name = "项目描述（使用了什么技术、解决了什么问题）")
    private String description;

    @SchemaProperty(name = "显示顺序")
    private Integer displayOrder;


}
