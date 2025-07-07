// =====================================================================================
// 2. ✅ 新增: 实时反馈DTO (RealtimeFeedbackDto.java)
// =====================================================================================
package com.echo.virtual_interview.model.dto.interview.process;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 实时反馈的数据传输对象 (DTO)

 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeFeedbackDto {
    //总结性反馈
    private String summary;
    //改进建议
    private String suggestion;
    //状态标识 (POSITIVE 或 NEEDS_IMPROVEMENT)
    private String status;
    //用于后台存储的详细分析
    private String detailedAnalysis;

}