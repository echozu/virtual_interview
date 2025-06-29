package com.echo.virtual_interview.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 聊天会话表
 * </p>
 *
 * @author echo
 * @since 2025-06-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("chats")
@Schema(name="Chats对象", description="聊天会话表")
public class Chats implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "会话的唯一标识符，主键。推荐使用UUID格式，以保证全局唯一性。")
    @TableId(value = "chat_id", type = IdType.AUTO)
    private String chatId;

    @SchemaProperty(name = "外键，关联到用户表(users)的主键ID，用于标识该会话属于哪个用户。")
    @TableField("user_id")
    private Long userId;

    @SchemaProperty(name = "会话的标题，方便用户识别，此标题用户可以修改。")
    private String title;

    @SchemaProperty(name = "系统级提示(System Prompt)，用于设定AI在该会话中的角色、背景或行为准则。")
    private String systemPrompt;

    @SchemaProperty(name = "会话的创建时间，在插入记录时默认为当前服务器时间。")
    private LocalDateTime createdAt;

    @SchemaProperty(name = "会话的最后更新时间。每当记录更新时，会自动更新为当前时间戳。主要用于会话列表的排序。")
    private LocalDateTime updatedAt;

    @SchemaProperty(name = "软删除标记,0表示为删除")
    private Integer isDeleted;


}
