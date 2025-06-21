package com.echo.virtual_interview.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
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
 * 简历主表，整合了大部分信息
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("resumes")
@Schema(name="Resumes对象", description="简历主表，整合了大部分信息")
public class Resumes implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "简历唯一ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "所属用户的ID")
    private Long userId;

    @SchemaProperty(name = "简历标题")
    private String title;

    @SchemaProperty(name = "姓名")
    private String fullName;

    @SchemaProperty(name = "性别")
    private String gender;

    @SchemaProperty(name = "年龄")
    private Integer age;

    @SchemaProperty(name = "年级")
    private String grade;

    @SchemaProperty(name = "电话")
    private String phoneNumber;

    @SchemaProperty(name = "邮箱")
    private String email;

    @SchemaProperty(name = "个人网站或作品集链接")
    private String personalWebsite;

    @Schema(description = "自定义字段（JSON对象格式）",
            example = "{\"GitHub\": \"https://github.com/username\", \"LinkedIn\": \"https://linkedin.com/in/username\"}")
    private String customFields;

    @SchemaProperty(name = "学历")
    private String educationLevel;

    @SchemaProperty(name = "学校名称")
    private String schoolName;

    @SchemaProperty(name = "专业名称")
    private String major;

    @SchemaProperty(name = "教育经历开始日期")
    private LocalDate educationStartDate;

    @SchemaProperty(name = "教育经历结束日期")
    private LocalDate educationEndDate;

    @SchemaProperty(name = "教育经历是否至今")
    private Boolean educationIsCurrent;

    @SchemaProperty(name = "教育经历详细描述")
    private String educationDescription;

    @SchemaProperty(name = "相关技能描述")
    private String skillsDescription;

    @SchemaProperty(name = "荣誉证书描述")
    private String honorsDescription;

    @SchemaProperty(name = "个人评价内容")
    private String evaluationContent;

    @Schema(description = "简历各模块显示顺序（JSON数组格式）",
            example = "[\"education\", \"skills\", \"projects\", \"honors\", \"evaluation\"]")
    private String sectionOrder;

    @SchemaProperty(name = "创建时间")
    private LocalDateTime createdAt;

    @SchemaProperty(name = "更新时间")
    private LocalDateTime updatedAt;


}
