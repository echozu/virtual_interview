package com.echo.virtual_interview.model.dto.experience;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 面经列表的视图对象 (View Object)
 */
@Data
public class ExperiencePostVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;

    // 作者信息
    private Integer authorId;
    private String authorNickname;
    private String authorAvatar;

    // 摘要信息
    private String summary; // 内容摘要，截取前100个字符
    private List<String> tags; // 标签

    // 统计数据
    private Integer viewsCount;
    private Integer likesCount;
    private Integer commentsCount;

    private LocalDateTime createdAt;

    /**
     * 这个字段仅用于接收MyBatis从数据库返回的GROUP_CONCAT字符串
     * 它不是给前端的。
     */
    @JsonIgnore // 确保这个字段不会被序列化到前端
    private String tagsString;

    /**
     * 重写getter方法，在获取tags时，如果tags为空，则根据tagsString进行转换
     *
     * @return 标签列表
     */
    public List<String> getTags() {
        if (this.tags == null && this.tagsString != null && !this.tagsString.isEmpty()) {
            this.tags = Arrays.asList(this.tagsString.split(","));
        }
        return this.tags;
        // 可以添加AI报告中的雷达图数据等关键信息
        // private Object radarChartData;
    }
}