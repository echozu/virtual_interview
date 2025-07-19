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
     * 1.2 在分享面试分析记录时，获取可选的、可展示/隐藏的报告模块列表
     * @return 可分享模块的列表
     */
    @GetMapping("/get/elements")
    public BaseResponse<List<ShareableElementDTO>> getShareableReportElements() {
        List<ShareableElementDTO> elements = experiencePostService.getShareableElements();
        return ResultUtils.success(elements);
    }
    /**
     * 1.3. 在面试广场时，获取历史的面试记录 方便为发表的面经指定对应的面试
     */
    @GetMapping("/history/list")
    public BaseResponse<List<InterviewHistoryDTO>> getHistory() {
        Integer userId = UserIdContext.getUserIdContext();
        List<InterviewHistoryDTO> historyList = experiencePostService.getHistoryWithExperience(userId);
        return ResultUtils.success(historyList);
    }

    /**
     * 3.获取面经详情
     * @param postId 面经的唯一ID
     * @return 包含完整信息的面经详情
     */
    @GetMapping("/get/{postId}")
    public BaseResponse<ExperiencePostDetailResponse> getExperiencePostDetail(@PathVariable Long postId) {
        ExperiencePostDetailResponse detail = experiencePostService.getExperiencePostDetail(postId);
        return ResultUtils.success(detail);
    }

    /**
     * 4.分页获取面经列表
     * @param queryRequest 包含分页、排序和筛选条件的请求体
     * @return 分页的面经列表视图
     */
    @PostMapping("/list")
    public BaseResponse<Page<ExperiencePostVO>> listPosts(@RequestBody ExperiencePostQueryRequest queryRequest) {
        Page<ExperiencePostVO> pageResult = experiencePostService.listExperiencePostsByPage(queryRequest);
        return ResultUtils.success(pageResult);
    }
    
    /**
     * 删除一篇面经
     * @param postId 面经的唯一ID
     * @return 操作成功响应
     */
    @DeleteMapping("/{postId}")
    public BaseResponse<Boolean> deletePost(@PathVariable Long postId) {
        experiencePostService.deleteExperiencePost(postId);
        return ResultUtils.success(true);
    }
}