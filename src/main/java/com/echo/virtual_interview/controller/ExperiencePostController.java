package com.echo.virtual_interview.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.model.dto.experience.*;
import com.echo.virtual_interview.model.dto.history.InterviewHistoryCardDTO;
import com.echo.virtual_interview.service.IExperiencePostsService;
import com.echo.virtual_interview.service.IInterviewService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 面经广场 API
 */
@RestController
@RequestMapping("/api/experience/posts")
public class ExperiencePostController {

    @Resource
    private IExperiencePostsService experiencePostService;


    /**
     * 1. 在查看面试分析报告时的创建一篇新的面经
     * @param createRequest 包含面经内容的请求体
     * @return 新创建的面经ID
     */
    @PostMapping("/creat")
    public BaseResponse<Long> createExperiencePost(@RequestBody ExperiencePostCreateRequest createRequest) {
        Long postId = experiencePostService.createExperiencePost(createRequest);
        return ResultUtils.success(postId);
    }
    /**
     * 1.1 在创建面经的分享面试分析记录时，获取可选的的分析报告列表
     * 目的是获取分析报告的sessionId
     * @return 可分享模块的列表
     */
    @GetMapping("/get/elements")
    public BaseResponse<List<ShareableElementDTO>> getShareableReportElements() {
        List<ShareableElementDTO> elements = experiencePostService.getShareableElements();
        return ResultUtils.success(elements);
    }
    /**
     * 1.2 在面试广场时，创建面经时，获取历史的分析报告，方便为发表的面经指定对应的面试
     */
    @GetMapping("/history/list")
    public BaseResponse<List<InterviewHistoryDTO>> getHistory() {
        Integer userId = UserIdContext.getUserIdContext();
        List<InterviewHistoryDTO> historyList = experiencePostService.getHistoryWithExperience(userId);
        return ResultUtils.success(historyList);
    }
    /**
     * 2.获取面经广场首页的排行榜数据
     * @return 包含多个排行榜的聚合数据
     */
    @GetMapping("/get/leaderboard")
    public BaseResponse<LeaderboardResponse> getLeaderboards() {
        LeaderboardResponse leaderboards = experiencePostService.getLeaderboards();
        return ResultUtils.success(leaderboards);
    }
    /**
     * 3.1获取分类信息
     */
    @GetMapping("/filters")
    public BaseResponse<Map<String, Object>> getExperienceFilterOptions() {
        return ResultUtils.success(experiencePostService.getExperienceFilterOptions());
    }
    /**
     * 3.2 分页获取面经列表
     * @param queryRequest 包含分页、排序和筛选条件的请求体
     * @return 分页的面经列表视图
     */
    @PostMapping("/list")
    public BaseResponse<Page<ExperiencePostVO>> listPosts(@RequestBody ExperiencePostQueryRequest queryRequest) {
        Page<ExperiencePostVO> pageResult = experiencePostService.listExperiencePostsByPage(queryRequest);
        return ResultUtils.success(pageResult);
    }
    /**
     * 4.获取面经详情
     * 获取该发布者的分析报告+面经详情、差评论区等
     * @param postId 面经的唯一ID
     * @return 包含完整信息的面经详情
     */
    @GetMapping("/get/{postId}")
    public BaseResponse<ExperiencePostDetailResponse> getExperiencePostDetail(@PathVariable Long postId) {
        ExperiencePostDetailResponse detail = experiencePostService.getExperiencePostDetail(postId);
        return ResultUtils.success(detail);
    }
    /**
     * 5.删除一篇面经
     * @param postId 面经的唯一ID
     * @return 操作成功响应
     */
    @DeleteMapping("/{postId}")
    public BaseResponse<Boolean> deletePost(@PathVariable Long postId) {
        experiencePostService.deleteExperiencePost(postId);
        return ResultUtils.success(true);
    }
}