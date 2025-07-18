package com.echo.virtual_interview.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 存储每次面试后的个性化学习推荐
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("interview_learning_recommendations")
@Schema(name="InterviewLearningRecommendations对象", description="存储每次面试后的个性化学习推荐")
public class InterviewLearningRecommendations implements Serializable {

    private static final long serialVersionUID = 1L;

    @SchemaProperty(name = "推荐记录唯一ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "关联的面试会话ID")
    private String sessionId;

    @SchemaProperty(name = "推荐的学习资源ID")
    private Long resourceId;

    @SchemaProperty(name = "推荐理由 (由AI生成，解释为何推荐此资源)")
    private String recommendationReason;

    @SchemaProperty(name = "推荐学习步骤/优先级")
    private Integer stepNumber;

    @SchemaProperty(name = "创建时间")
    private LocalDateTime createdAt;


}
