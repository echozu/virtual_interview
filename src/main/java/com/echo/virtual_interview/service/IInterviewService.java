package com.echo.virtual_interview.service;

import com.echo.virtual_interview.model.dto.history.InterviewHistoryCardDTO;
import com.echo.virtual_interview.model.dto.interview.process.RealtimeFeedbackDto;
import com.echo.virtual_interview.model.dto.interview.process.VideoAnalysisPayload;
import reactor.core.publisher.Flux;

import java.util.List;

public interface IInterviewService {
    Flux<String> interviewProcess(String message, String sessionId, Integer userId);

    void end(Integer userId,String sessionId);

    String start(Long channelId);

    RealtimeFeedbackDto processAndStoreAnalysis(VideoAnalysisPayload payload);

    void generateAndSendGreetingAudio(String sessionId, Integer userId);

    List<InterviewHistoryCardDTO> getHistoryForUser(Integer userId);
}
