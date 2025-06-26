package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.controller.ai.InterviewExpert;
import com.echo.virtual_interview.model.dto.interview.ChannelDetailDTO;
import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import com.echo.virtual_interview.model.entity.InterviewChannels;
import com.echo.virtual_interview.model.entity.InterviewSessions;
import com.echo.virtual_interview.model.entity.ResumeModule;
import com.echo.virtual_interview.service.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class IInterviewServiceImpl implements IInterviewService {

    @Resource
    private IResumeService resumeService;
    @Resource
    private IInterviewSessionsService sessionsService;
    @Resource
    private IInterviewChannelsService channelsService;
    @Resource
    private IResumeModuleService resumeModuleService;

    @Resource
    private InterviewExpert interviewExpert;
    @Override
    public Flux<String> interviewProcess(String message, String sessionId) {
        Integer userId = UserIdContext.getUserIdContext();

        // 1. 获取简历信息
        ResumeDataDto resume = resumeService.getResumeByUserId(userId);
        Long resumeId= resumeService.getResumeIdByUserId(userId);
        //根据简历id获取对应的model
        List<ResumeModule> resumeModules = resumeModuleService.getResumeModulesByResumId(resumeId);

        // 2. 获取面试会话
        InterviewSessions session = sessionsService.getById(sessionId);
        if (session == null) {
            return Flux.error(new IllegalArgumentException("无效的会话ID"));
        }

        // 3. 获取频道信息
        ChannelDetailDTO channel = channelsService.getChannelDetailsNoAdd(session.getChannelId());
        // 4. 调用 AI 对话（注入上下文）
        return interviewExpert.doChatByStreamWithProcess(message, sessionId, resume,resumeModules,channel);
    }




}
