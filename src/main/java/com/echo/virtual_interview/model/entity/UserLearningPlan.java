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
 * 用户的综合个性化学习任务清单
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_learning_plan")
@Schema(name="UserLearningPlan对象", description="用户的综合个性化学习任务清单")
public class UserLearningPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "用户ID")
    private Long userId;

    @SchemaProperty(name = "学习资源ID")
    private Long resourceId;

    @SchemaProperty(name = "来源的分析报告ID（可选）")
    private Long sourceReportId;

    @SchemaProperty(name = "任务完成状态('待完成', '进行中', '已完成')")
    private String status;

    @SchemaProperty(name = "添加时间")
    private LocalDateTime addedAt;

    @SchemaProperty(name = "完成时间")
    private LocalDateTime completedAt;


}
