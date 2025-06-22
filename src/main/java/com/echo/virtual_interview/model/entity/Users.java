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
 * 存储平台的用户基本信息
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("users")
@Schema(name="Users对象", description="存储平台的用户基本信息")
public class Users implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "用户唯一ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "用户名（用于登录）")
    private String username;

    @SchemaProperty(name = "哈希后的密码")
    private String password;

    @SchemaProperty(name = "邮箱（用于注册和通知）")
    private String email;

    @SchemaProperty(name = "用户昵称")
    private String nickname;

    @SchemaProperty(name = "用户头像URL")
    private String avatarUrl;

    @SchemaProperty(name = "创建时间")
    @TableField(fill = FieldFill.INSERT) // <<--- 添加此注解
    private LocalDateTime createdAt;

    @SchemaProperty(name = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE) // <<--- 添加此注解
    private LocalDateTime updatedAt;

    @SchemaProperty(name = "用户角色：user/admin/ban")
    private String role;

    @SchemaProperty(name = "用户的总积分-注册赠送200积分，面试一次消耗10积分，每天登录赠送100积分")
    private Integer points;
    @SchemaProperty(name = "用户简介")
    private String profile;

}
