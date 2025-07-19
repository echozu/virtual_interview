package com.echo.virtual_interview.controller;

import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.model.dto.analysis.InterviewReportResponseDTO;
import com.echo.virtual_interview.model.dto.history.InterviewHistoryCardDTO;
import com.echo.virtual_interview.model.dto.interview.process.RealtimeFeedbackDto;
import com.echo.virtual_interview.model.dto.interview.process.VideoAnalysisPayload;
import com.echo.virtual_interview.service.IAnalysisReportsService;
import com.echo.virtual_interview.service.IInterviewService;
import com.echo.virtual_interview.service.IInterviewSessionsService;
import com.echo.virtual_interview.service.impl.AnalysisStatusService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 面试接口
 */
@RestController
@RequestMapping("/api/interview")
@Slf4j
public class InterviewController {

    @Resource
    private AnalysisStatusService analysisStatusService;
    @Resource
    private IInterviewService interviewService;
    @Resource
    private IAnalysisReportsService analysisReportsService;

    /**
     * 开启面试
     * 这里去创建一个新的session，方便接下来的面试存储
     *
     * @param channelId 所选中的频道ID (required)
     * @return 包含sessionId的响应实体
     */
    @GetMapping("/process/start")
    public BaseResponse<String> interviewStart(@RequestParam Long channelId) {
        String sessionId = interviewService.start(channelId);
        return ResultUtils.success(sessionId);
    }

    /**
     * 触发“面试开始”流程。
     * 此接口会立即返回，并在后台异步生成开场白语音，通过WebSocket推送给用户。
     * @param sessionId 面试会话ID
     * @return 返回一个表示任务已成功启动的响应
     */
    @GetMapping("/process/start/greeting-audio")
    public BaseResponse<String> triggerGreetingAudio(
            @RequestParam String sessionId) {
        Integer userId = UserIdContext.getUserIdContext();
        // 2. 调用ai回复的逻辑
        interviewService.generateAndSendGreetingAudio(sessionId, userId);

        // 3. 立即返回成功响应，告知前端 "任务已接收，请等待WebSocket推送"
        return ResultUtils.success("请等待websocket响应.");
    }
    @Autowired
    @Qualifier("videoAnalysisExecutor")
    private ExecutorService videoAnalysisExecutor;
    /**
     * 接收视频分析结果。
     * 此接口由Python分析服务调用。
     */
    @PostMapping("/process/python/video_analyse")
    public BaseResponse<String> receiveVideoAnalysis(@RequestBody VideoAnalysisPayload payload) {
        System.out.println("成功接收到 analysisId 的分析结果: " + payload.getAnalysisId());

        // 使用单线程执行器提交任务，而不是 new Thread()
        videoAnalysisExecutor.submit(() -> {
            try {
                // 这里的代码块会进入任务队列，等待唯一的那个线程来执行
                System.out.println("开始处理 analysisId: " + payload.getAnalysisId());
                RealtimeFeedbackDto finalResult = interviewService.processAndStoreAnalysis(payload);
                analysisStatusService.updateTaskResult(payload.getAnalysisId(), "COMPLETED", finalResult);
                System.out.println("成功处理 analysisId: " + payload.getAnalysisId());
            } catch (Exception e) {
                analysisStatusService.updateTaskResult(payload.getAnalysisId(), "FAILED", e.getMessage());
                System.err.println("处理 analysisId " + payload.getAnalysisId() + " 时发生异步错误: " + e.getMessage());
            }
        });

        // 立即返回，告诉Python任务已接收
        return ResultUtils.success("数据已成功接收，正在后台排队处理。");
    }

    /**
     * 视频分析的轮询接口 (这个接口保持不变)
     */
    @GetMapping("/process/analysis/result/{analysisId}")
    public BaseResponse<AnalysisStatusService.AnalysisResult> getAnalysisResult(@PathVariable String analysisId) {
        AnalysisStatusService.AnalysisResult result = analysisStatusService.getTaskResult(analysisId);
        if (result == null) {
            // 这里可以稍微优化一下，如果任务确实还没开始，可以给一个更明确的提示
            // 但目前的逻辑也是可以的
            return ResultUtils.success(new AnalysisStatusService.AnalysisResult("PENDING", "分析任务正在队列中，请稍候..."));
        }
        return ResultUtils.success(result);
    }

    /**
     * 结束面试-异步
     * 1.当前端用户点击结束面试时，前端/安卓端自己控制去断开ws连接，释放资源： stompClient.deactivate(); // 或者 stompClient.disconnect()
     * 2.其他的逻辑在后台操作
     * @return
     */
    @GetMapping("/process/end")
    public BaseResponse<String> getHistorySessionCard(@RequestParam String sessionId) {
        Integer userId = UserIdContext.getUserIdContext();

        interviewService.end(userId,sessionId);
        return ResultUtils.success("结束面试成功");
    }
    /**
     * 获取当前用户的面试历史记录列表
     * @return 包含面试历史卡片信息的列表
     */
    @GetMapping("/history/list")
    public BaseResponse<List<InterviewHistoryCardDTO>> getHistory() {
        Integer userId = UserIdContext.getUserIdContext();
        List<InterviewHistoryCardDTO> historyList = interviewService.getHistoryForUser(userId);
        return ResultUtils.success(historyList);
    }
    /**
     * 获取指定面试会话的完整分析报告-有缓存
     * @param sessionId 面试会话的唯一ID
     * @return 包含所有分析数据的JSON响应
     */
    @GetMapping("/analysis/{sessionId}")
    public BaseResponse<InterviewReportResponseDTO> getReport(@PathVariable String sessionId) {
        InterviewReportResponseDTO report = analysisReportsService.getFullReportBySessionId(sessionId);
        return ResultUtils.success(report);
    }

/*    *//**
     * 获取当前用户的面试历史记录卡片（只返回一些重要信息 方便前端展示卡片
     *//*
    @GetMapping("/history/list/card")
    public BaseResponse<String> getHistorySessionCard() {
        Integer userId = UserIdContext.getUserIdContext();
        interviewService.getHistorySessionByUserId();

    }
    *//**
     * 根据sessionid，得到面试的具体分析情况
     *//*
    @GetMapping("/history/analysis/detail")
    public BaseResponse<String> getHistorySessionDetail(@RequestParam String sessionId) {
        Integer userId = UserIdContext.getUserIdContext();
        interviewService.getHistorySessionByUserId();

    }*/
}

/*    *//**
     * 面试过程-sse-测试
     *
     * @param message   用户信息
     * @param sessionId 聊天唯一id
     * @return 返回信息
     *//*
    @GetMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> interviewProcess(String message, String sessionId) {
        // 获取当前登录用户
        Integer userId = UserIdContext.getUserIdContext();
        return interviewService.interviewProcess(message, sessionId, userId);
    }*/

