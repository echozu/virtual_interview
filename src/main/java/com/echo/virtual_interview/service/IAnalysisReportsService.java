package com.echo.virtual_interview.service;

import com.echo.virtual_interview.model.dto.analysis.InterviewReportResponseDTO;
import com.echo.virtual_interview.model.entity.AnalysisReports;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 存储详细的面试后分析报告 服务类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
public interface IAnalysisReportsService extends IService<AnalysisReports> {
    /**
     * 根据面试会话ID获取完整的分析报告
     * @param sessionId 会话ID
     * @return 完整的报告DTO，用于前端展示
     */
    InterviewReportResponseDTO getFullReportBySessionId(String sessionId);
}
