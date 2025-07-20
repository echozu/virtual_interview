package com.echo.virtual_interview.model.dto.experience;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面经帖子卡片视图对象
 */
@Data
public class ExperiencePostVO {
    
    /**
     * 帖子ID
     */
    private Long postId;
    
    /**
     * 作者昵称
     */
    private String authorNickname;
    /**
     * 作者头像
     */
    private String avatarUrl;

    /**
     * 贴子的总结
     */
    private String summary;
    /**
     * 面经的创建时间
     */
    private LocalDateTime createdAt; // 创建时间

    /**
     * 标题
     */
    private String title;
    
    /**
     * 标签列表 (将在Java代码中转换)
     */
    private List<String> tags;
    
    /**
     * 点赞数
     */
    private Integer likesCount;
    
    /**
     * 评论数
     */
    private Integer commentsCount;
    
    /**
     * 应聘岗位
     */
    private String position;
    /**
     * 封面链接
     */
    private String experienceUrl;
    /**
     * 公司
     */
    private String company;
    
    /**
     * 工作类型
     */
    private String jobType;

    /**
     * 是否匿名 (用于前端判断是否显示作者详情链接)
     */
    private Boolean isAnonymous;
    
    /**
     * 标签的JSON字符串 (从DB查询出的原始值，用于在Java中后处理)
     * 这个字段不返回给前端
     */
    @JsonIgnore
    private String tagsJson;


}