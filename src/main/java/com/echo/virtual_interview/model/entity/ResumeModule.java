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
 * 简历模块表
 * </p>
 *
 * @author echo
 * @since 2025-06-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("resume_module")
@Schema(name="ResumeModule对象", description="简历模块表")
public class ResumeModule implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "模块主键ID (自增)")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "所属简历ID (逻辑关联，无外键约束)")
    private Long resumeId;

    @SchemaProperty(name = "父模块ID (0表示顶层模块，用于项目经历等层级关系)")
    private Long parentId;

    @SchemaProperty(name = "模块类型 (例如: BASIC_INFO, EDUCATION, PROJECT等)")
    private String moduleType;

    @SchemaProperty(name = "模块排序值 (在同一父模块下的显示顺序)")
    private Integer sort;

    @SchemaProperty(name = "基础信息的“键” (如: 姓名, 期望岗位)")
    private String itemKey;

    @SchemaProperty(name = "基础信息的“值” (如: 张同学, 全栈开发)")
    private String itemValue;

    @SchemaProperty(name = "模块标题 (如: 教育经历)")
    private String title;

    @SchemaProperty(name = "主体名称 (如: 学校名, 项目名)")
    private String subject;

    @SchemaProperty(name = "专业或角色 (如: 软件工程, 全栈开发)")
    private String major;

    @SchemaProperty(name = "开始日期 (格式由前端定，如 2021/09/01)")
    private String fromDate;

    @SchemaProperty(name = "结束日期 (格式由前端定，如 2025/07/01)")
    private String toDate;

    @SchemaProperty(name = "是否至今")
    private Boolean toNow;

    @SchemaProperty(name = "详细描述内容 (支持Markdown/HTML)")
    private String content;

    @SchemaProperty(name = "是否在简历中显示")
    private Boolean isAppear;

    @SchemaProperty(name = "创建时间")
    private LocalDateTime createTime;

    @SchemaProperty(name = "最后更新时间")
    private LocalDateTime updateTime;


}
