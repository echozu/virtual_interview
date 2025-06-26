package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import com.echo.virtual_interview.model.entity.InterviewSessions;
import com.echo.virtual_interview.mapper.InterviewSessionsMapper;
import com.echo.virtual_interview.service.IInterviewSessionsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class InterviewSessionsServiceImpl extends ServiceImpl<InterviewSessionsMapper, InterviewSessions>
        implements IInterviewSessionsService {

    @Resource
    private ResumeServiceImpl resumeService;
    @Resource
    private InterviewSessionsMapper interviewSessionsMapper;
    @Override
    public String start(Long channelId) {
        Integer userId = UserIdContext.getUserIdContext();
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 获取简历ID（处理可能的空值）
        Long resumeId = Optional.ofNullable(resumeService.getResumeIdByUserId(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.OPERATION_ERROR,"请先上传简历"));

        // 创建会话ID
        String sessionId = String.format("%d-%s-%d",
                userId, channelId, System.currentTimeMillis());

        // 保存会话记录
        InterviewSessions session = new InterviewSessions()
                .setId(sessionId)
                .setChannelId(channelId)
                .setUserId(userId.longValue())
                .setResumeId(resumeId)
                .setStatus("进行中");

        interviewSessionsMapper.insert(session);
        return sessionId;
    }
}
