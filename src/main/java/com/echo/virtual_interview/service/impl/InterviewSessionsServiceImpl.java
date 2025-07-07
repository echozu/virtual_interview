package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.controller.ai.InterviewExpert;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.mapper.InterviewAnalysisChunkMapper;
import com.echo.virtual_interview.model.dto.interview.process.IflytekExpressionResponse;
import com.echo.virtual_interview.model.dto.interview.process.RealtimeFeedbackDto;
import com.echo.virtual_interview.model.dto.interview.process.VideoAnalysisPayload;
import com.echo.virtual_interview.model.entity.InterviewAnalysisChunk;
import com.echo.virtual_interview.model.entity.InterviewSessions;
import com.echo.virtual_interview.mapper.InterviewSessionsMapper;
import com.echo.virtual_interview.service.IInterviewSessionsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.echo.virtual_interview.utils.face.IflytekExpressionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * 记录每一次具体的面试实例 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Service
@Transactional
@Slf4j
public class InterviewSessionsServiceImpl extends ServiceImpl<InterviewSessionsMapper, InterviewSessions>
        implements IInterviewSessionsService {


}
