package com.echo.virtual_interview.model.dto.comment;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List; /**
 * 评论视图对象 (支持树状结构)
 */
@Data
public class CommentVO {

    private Long id;
    private String content;
    private LocalDateTime createdAt;

    /**
     * 评论的作者信息
     */
    private UserInfo author;

    /**
     * 这条评论下的所有回复（楼中楼）
     */
    private List<CommentVO> replies;

    /**
     * 作者信息嵌套对象
     */
    @Data
    public static class UserInfo {
        private Long userId;
        private String nickname;
        private String avatarUrl;
    }
}
