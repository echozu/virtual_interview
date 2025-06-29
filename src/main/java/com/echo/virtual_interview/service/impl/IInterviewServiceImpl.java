package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.controller.ai.InterviewExpert;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelDetailDTO;
import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
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
    public Flux<String> interviewProcess(String message, String sessionId, Integer userId) {
        // 1. 获取简历
        ResumeDataDto resume = resumeService.getResumeByUserId(userId);
        Long resumeId = resumeService.getResumeIdByUserId(userId);
        List<ResumeModule> resumeModules = resumeModuleService.getResumeModulesByResumId(resumeId);

        // 2. 会话校验
        InterviewSessions session = sessionsService.getById(sessionId);
        if (session == null) {
            return Flux.error(new IllegalArgumentException("无效的会话ID"));
        }

        // 3. 频道信息
        ChannelDetailDTO channel = channelsService.getChannelDetailsNoAdd(session.getChannelId());

        // 4. AI 处理（流式返回）
        System.out.println("用户信息："+message);
        return interviewExpert.aiInterviewByStreamWithProcess(message, sessionId, resume, resumeModules, channel);
    }





}
