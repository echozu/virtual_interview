package com.echo.virtual_interview.model.dto.experience;

import lombok.Data;

import java.util.List;

/**
 * 排行榜中的热门帖子信息
 */
@Data
public class HotPostDTO {
    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 帖子标题
     */
    private String title;
    private String experienceUrl;
    private List<String> tags;
    // 热度值
    private Integer likesCount;
}