package com.echo.virtual_interview.controller;

import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.model.dto.interview.process.RealtimeFeedbackDto;
import com.echo.virtual_interview.model.dto.interview.process.VideoAnalysisPayload;
import com.echo.virtual_interview.service.IInterviewService;
import com.echo.virtual_interview.service.IInterviewSessionsService;
import com.echo.virtual_interview.service.impl.AnalysisStatusService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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
     * @param userId 用户ID
     * @return 返回一个表示任务已成功启动的响应
     */
    @GetMapping("/process/start/greeting-audio")
    public BaseResponse<String> triggerGreetingAudio(
            @RequestParam String sessionId,
            @RequestParam Integer userId) { // 1. 新增userId参数

        // 2. 调用ai回复的逻辑
        interviewService.generateAndSendGreetingAudio(sessionId, userId);

        // 3. 立即返回成功响应，告知前端 "任务已接收，请等待WebSocket推送"
        return ResultUtils.success("请等待websocket响应.");
    }

    /**
     * 接收视频分析结果，包含行为数据、关键帧图像和 sessionId。
     * 此接口由Python分析服务调用。
     *
     * @param payload 包含所有分析结果的完整JSON负载，自动映射为Java对象。
     * @return 一个统一的响应对象，告知Python服务是否接收成功。
     */

    @PostMapping("/process/python/video_analyse")
    public BaseResponse<String> receiveVideoAnalysis(@RequestBody VideoAnalysisPayload payload) {
        System.out.println("成功接收到 analysisId 的分析结果: " + payload.getAnalysisId());

        new Thread(() -> {
            try {
                RealtimeFeedbackDto finalResult = interviewService.processAndStoreAnalysis(payload);
                analysisStatusService.updateTaskResult(payload.getAnalysisId(), "COMPLETED", finalResult);
            } catch (Exception e) {
                analysisStatusService.updateTaskResult(payload.getAnalysisId(), "FAILED", e.getMessage());
                System.err.println("处理 analysisId " + payload.getAnalysisId() + " 时发生异步错误: " + e.getMessage());
            }
        }).start();

        return ResultUtils.success("数据已成功接收，正在后台处理。");
    }


    /**
     * 视频分析的轮询接口
     * 前端通过此接口，使用 analysisId 轮询分析结果。
     */
    @GetMapping("/process/analysis/result/{analysisId}")
    public BaseResponse<AnalysisStatusService.AnalysisResult> getAnalysisResult(@PathVariable String analysisId) {
        AnalysisStatusService.AnalysisResult result = analysisStatusService.getTaskResult(analysisId);
        if (result == null) {
            return ResultUtils.success(new AnalysisStatusService.AnalysisResult("PENDING", "分析任务正在队列中，请稍候..."));
        }
        return ResultUtils.success(result);
    }

    /**
     * 结束面试
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

