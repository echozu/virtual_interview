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
 * 简历主表
 * </p>
 *
 * @author echo
 * @since 2025-06-22
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("resume")
@Schema(name="Resume对象", description="简历主表")
public class Resume implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "简历主键ID (自增)")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "所属用户ID (逻辑关联，无外键约束)")
    private Long userId;

    @SchemaProperty(name = "简历标题 (来自basicInfo.title)")
    private String title;

    @SchemaProperty(name = "简历封面图URL (来自basicInfo.cover)")
    private String cover;

    @SchemaProperty(name = "简历标签 (来自basicInfo.tag)")
    private String tag;

    @SchemaProperty(name = "用户头像URL (来自basicInfo.avatar)")
    private String avatar;

    @SchemaProperty(name = "创建时间")
    private LocalDateTime createTime;

    @SchemaProperty(name = "最后更新时间")
    private LocalDateTime updateTime;


}
