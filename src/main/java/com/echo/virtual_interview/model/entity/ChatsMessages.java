package com.echo.virtual_interview.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
 * 存储每个会话中的具体聊天记录
 * </p>
 *
 * @author echo
 * @since 2025-06-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("chats_messages")
@Schema(name="ChatsMessages对象", description="存储每个会话中的具体聊天记录")
public class ChatsMessages implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "消息的唯一ID，设为自增主键。")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "外键，关联到会话表(chats)的chatId，用于标识该消息属于哪个会话。")
    @TableField("chat_id")
    private String chatId;

    @SchemaProperty(name = "消息发送者的角色，例如 'user' (用户) 或 'assistant'。")
    private String role;

    @SchemaProperty(name = "消息的具体文本内容。")
    private String content;

    @SchemaProperty(name = "消息的创建时间，在插入记录时默认为当前服务器时间。")
    private LocalDateTime createdAt;


}
