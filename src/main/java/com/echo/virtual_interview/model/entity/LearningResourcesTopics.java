package com.echo.virtual_interview.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 学习资源的知识点/主题库
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("learning_resources_topics")
@Schema(name="LearningResourcesTopics对象", description="学习资源的知识点/主题库")
public class LearningResourcesTopics implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "知识点唯一ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @SchemaProperty(name = "知识点名称 (如: Java并发, Redis持久化, Vue3响应式原理)")
    private String name;

    @SchemaProperty(name = "知识点简要描述")
    private String description;

    @SchemaProperty(name = "知识点分类 (如: 后端技术, 前端技术, 数据库, 软技能)")
    private String category;


}
