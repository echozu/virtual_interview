// --- DTO for creating a comment ---
package com.echo.virtual_interview.model.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建评论的请求体
 */
@Data
public class CommentCreateRequest {

    /**
     * 评论内容，不能为空
     */
    @NotBlank(message = "评论内容不能为空")
    private String content;

    /**
     * 父评论ID。
     * 如果是对主帖直接评论，则此字段为null或不传。
     * 如果是回复某条评论，则此字段为被回复评论的ID。
     */
    private Long parentCommentId;
}



