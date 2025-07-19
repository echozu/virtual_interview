package com.echo.virtual_interview.model.dto.experience;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 创建面经的请求体
 */
@Data
public class ExperiencePostCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联的面试会话ID
     */
    private String sessionId;

    /**
     * 面经标题
     */
    private String title;

    /**
     * 用户撰写的复盘内容
     */
    private String content;
    
    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 可见性 (PUBLIC, PRIVATE)
     */
    private String visibility;

    /**
     * 是否匿名发布
     */
    private Boolean isAnonymous;

    /**
     * 选择性分享的报告元素。
     * <p>
     * 该字段用于存储用户自定义的分享偏好，决定AI分析报告中的哪些模块可以公开展示。
     * 它对应数据库中的JSON类型字段。
     * <p>
     * <b>例如:</b> 一个用户可能只想分享他的能力雷达图和改进建议，但隐藏具体的总体得分和紧张度曲线。
     * 此时，前端可以传一个如下结构的JSON对象，后端将其直接存入该字段：
     * <pre>
     * {
     * "show_overall_score": false,
     * "show_radar_chart": true,
     * "show_suggestions": true,
     * "show_tension_curve": false
     * }
     * </pre>
     */
    private Object sharedReportElements;
    /**
     * 面经的封面url
     */
    private String experienceUrl;
}