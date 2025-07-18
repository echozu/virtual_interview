package com.echo.virtual_interview.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.fasterxml.jackson.databind.node.BigIntegerNode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用于个性化推荐的学习资源库
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("learning_resources")
@Schema(name="LearningResources对象", description="用于个性化推荐的学习资源库")
public class LearningResources implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "资源唯一ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "关联的知识点ID")
    private Integer topicId;

    @SchemaProperty(name = "资源标题")
    private String title;

    @SchemaProperty(name = "资源描述")
    private String description;

    @SchemaProperty(name = "资源链接")
    private String url;

    @SchemaProperty(name = "资源类型(文章,视频,课程,书籍)")
    private String resourceType;

    @SchemaProperty(name = "难度等级(初级,中级,高级)")
    private String difficulty;

    @SchemaProperty(name = "创建时间")
    private LocalDateTime createdAt;


}
