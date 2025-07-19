package com.echo.virtual_interview.model.dto.experience;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 排行榜数据响应体
 */
@Data
@Builder
public class LeaderboardResponse {

    /**
     * 本周热门榜（按点赞数）
     */
    private List<HotPostDTO> weeklyHotPosts;

    /**
     * 本月贡献作者榜（按作者获赞数）
     */
    private List<TopAuthorDTO> monthlyTopAuthors;

    /**
     * 评论最多榜
     */
    private List<HotPostDTO> mostCommentedPosts;
}