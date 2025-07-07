package com.echo.virtual_interview.service;

import com.echo.virtual_interview.model.dto.interview.process.RealtimeFeedbackDto;
import com.echo.virtual_interview.model.dto.interview.process.VideoAnalysisPayload;
import com.echo.virtual_interview.model.entity.InterviewSessions;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * 记录每一次具体的面试实例 服务类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
public interface IInterviewSessionsService extends IService<InterviewSessions> {

}
