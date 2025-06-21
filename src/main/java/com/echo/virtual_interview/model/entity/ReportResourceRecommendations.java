package com.echo.virtual_interview.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 连接分析报告和推荐的学习资源（单场面试推荐）
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("report_resource_recommendations")
@Schema(name="ReportResourceRecommendations对象", description="连接分析报告和推荐的学习资源（单场面试推荐）")
public class ReportResourceRecommendations implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @SchemaProperty(name = "分析报告ID")
    private Long reportId;

    @SchemaProperty(name = "推荐的学习资源ID")
    private Long resourceId;

    @SchemaProperty(name = "推荐原因（如：针对STAR法则薄弱）")
    private String recommendationReason;

    @SchemaProperty(name = "学习路径中的步骤编号,用于前端利用x轴展示递进关系")
    private Integer stepNumber;


}
