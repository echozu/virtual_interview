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
 * 面经分享主表
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("experience_posts")
@Schema(name="ExperiencePosts对象", description="面经分享主表")
public class ExperiencePosts implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "面经唯一ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "作者的用户ID (逻辑关联 users.id)")
    private Long userId;

    @SchemaProperty(name = "关联的面试会话ID (逻辑关联 interview_sessions.id)")
    private String sessionId;

    @SchemaProperty(name = "面经标题")
    private String title;

    @SchemaProperty(name = "用户撰写的复盘内容 (支持富文本)")
    private String content;

    @SchemaProperty(name = "可见性 (PUBLIC:公开, PRIVATE:私有)")
    private String visibility;

    @SchemaProperty(name = "是否匿名发布 (0:否, 1:是)")
    private Boolean isAnonymous;

    @SchemaProperty(name = "状态 (PUBLISHED:已发布, UNDER_REVIEW:审核中, HIDDEN:已隐藏)")
    private String status;

    @SchemaProperty(name = "是否为精华帖 (0:否, 1:是)")
    private Boolean isFeatured;

    @SchemaProperty(name = "浏览量")
    private Integer viewsCount;

    @SchemaProperty(name = "点赞数")
    private Integer likesCount;
    @SchemaProperty(name = "标签列表")
    private String tags;
    @SchemaProperty(name = "面经的封面图片")
    private String experienceUrl;
    @SchemaProperty(name = "评论数")
    private Integer commentsCount;

    @SchemaProperty(name = "收藏数")
    private Integer collectionsCount;

    @SchemaProperty(name = "选择性分享的报告元素, 如: {'show_radar': true, 'show_score': false}")
    private String sharedReportElements;

    @SchemaProperty(name = "创建时间")
    private LocalDateTime createdAt;

    @SchemaProperty(name = "更新时间")
    private LocalDateTime updatedAt;
}
