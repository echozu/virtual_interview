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
 * 面经评论表
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("experience_comments")
@Schema(name="ExperienceComments对象", description="面经评论表")
public class ExperienceComments implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "评论唯一ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "关联的面经ID (逻辑关联 experience_posts.id)")
    private Long postId;

    @SchemaProperty(name = "评论者ID (逻辑关联 users.id)")
    private Long userId;

    @SchemaProperty(name = "父评论ID (逻辑自关联 experience_comments.id)")
    private Long parentCommentId;

    @SchemaProperty(name = "评论内容")
    private String content;

    @SchemaProperty(name = "创建时间")
    private LocalDateTime createdAt;

    @SchemaProperty(name = "更新时间")
    private LocalDateTime updatedAt;


}
