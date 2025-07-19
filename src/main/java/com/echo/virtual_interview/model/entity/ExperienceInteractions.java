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
 * 用户对文章的互动记录(点赞、收藏)
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("experience_interactions")
@Schema(name="ExperienceInteractions对象", description="用户对文章的互动记录(点赞、收藏)")
public class ExperienceInteractions implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "用户ID (逻辑关联 users.id)")
    private Long userId;

    @SchemaProperty(name = "面经ID (逻辑关联 experience_posts.id)")
    private Long postId;

    @SchemaProperty(name = "互动类型 (LIKE:点赞, COLLECT:收藏)")
    private String interactionType;

    @SchemaProperty(name = "互动时间")
    private LocalDateTime createdAt;


}
