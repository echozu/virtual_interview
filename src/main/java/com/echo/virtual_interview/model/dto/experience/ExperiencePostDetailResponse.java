package com.echo.virtual_interview.model.dto.experience;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // 如果字段为null，则不在JSON中系列化
public class ExperiencePostDetailResponse {

    private Long postId; // 面经ID
    private String title; // 标题
    private String content; // 用户撰写的复盘内容
    private List<String> tags; // 标签列表
    private String experienceUrl; // 封面图链接
    private LocalDateTime createdAt; // 创建时间

    private AuthorInfoDTO authorInfo; // 作者信息
    private PostStatsDTO stats; // 帖子统计数据
    private ViewerInteractionDTO viewerInteraction; // 当前查看者的互动状态
    private SharedAiReportDTO aiReport; // 经过筛选的AI报告

    @Data
    @Builder
    public static class AuthorInfoDTO {
        private Long userId; // 作者用户ID
        private String nickname; // 作者昵称
        private String avatar; // 作者头像
    }

    @Data
    @Builder
    public static class PostStatsDTO {
        private Integer viewsCount; // 浏览量
        private Integer likesCount; // 点赞数
        private Integer commentsCount; // 评论数
        private Integer collectionsCount; // 收藏数
    }

    @Data
    @Builder
    public static class ViewerInteractionDTO {
        private boolean isLiked; // 当前用户是否已点赞
        private boolean isCollected; // 当前用户是否已收藏
    }
}