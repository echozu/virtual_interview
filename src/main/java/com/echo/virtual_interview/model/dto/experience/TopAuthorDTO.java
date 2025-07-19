package com.echo.virtual_interview.model.dto.experience;

import lombok.Data;

/**
 * 排行榜中的热门作者信息
 */
@Data
public class TopAuthorDTO {
    /**
     * 作者用户ID
     */
    private Long userId;

    /**
     * 作者昵称
     */
    private String nickname;

    /**
     * 作者头像链接
     */
    private String avatarUrl;

    /**
     * 统计周期内的总获赞数
     */
    private Long totalLikes;
}