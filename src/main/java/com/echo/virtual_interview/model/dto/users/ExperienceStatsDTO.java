package com.echo.virtual_interview.model.dto.users;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户面经统计数据传输对象
 * 用于封装用户的总点赞数、总评论数、总收藏数和总面试数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceStatsDTO {

    /**
     * 用户收到的总点赞数
     */
    private Long totalLikes;

    /**
     * 用户收到的总评论数
     */
    private Long totalComments;

    /**
     * 用户收到的总收藏数
     */
    private Long totalCollections;

    /**
     * 用户发布的总面经数 (面试数)
     */
    private Long totalPosts;
}