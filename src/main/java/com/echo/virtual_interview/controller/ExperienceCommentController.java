package com.echo.virtual_interview.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.model.dto.comment.CommentCreateRequest;
import com.echo.virtual_interview.model.dto.comment.CommentVO;
import com.echo.virtual_interview.model.dto.comment.InteractionToggleResponse;
import com.echo.virtual_interview.service.IExperienceCommentsService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 面经评论与互动 API
 *
 * @author YourName
 */
@RestController
@RequestMapping("/api/experience/posts/{postId}")
public class ExperienceCommentController {

    @Resource
    private IExperienceCommentsService experienceCommentService;

    // ------------------- 评论相关功能 -------------------

    /**
     * 1. 分页获取面经的评论列表（树状结构）
     * @param postId 面经的唯一ID
     * @param current 当前页码
     * @param pageSize 每页数量
     * @return 分页的顶级评论列表，每个评论包含其回复
     */
    @GetMapping("/comments")
    public BaseResponse<Page<CommentVO>> listComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long pageSize) {
        Page<CommentVO> commentTreePage = experienceCommentService.getCommentTreePage(postId, current, pageSize);
        return ResultUtils.success(commentTreePage);
    }

    /**
     * 2. 发表一条新评论或回复
     * @param postId 面经的唯一ID
     * @param createRequest 包含评论内容和父评论ID的请求体
     * @return 新创建的评论VO
     */
    @PostMapping("/comments")
    public BaseResponse<CommentVO> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest createRequest) {
        CommentVO newComment = experienceCommentService.createComment(postId, createRequest);
        return ResultUtils.success(newComment);
    }

    // ------------------- 点赞/收藏相关功能 -------------------

    /**
     * 3. 点赞或取消点赞一个面经 (Toggle操作)
     * @param postId 面经的唯一ID
     * @return 操作后的点赞状态和最新的点赞总数
     */
    @PostMapping("/like")
    public BaseResponse<InteractionToggleResponse> toggleLike(@PathVariable Long postId) {
        InteractionToggleResponse response = experienceCommentService.toggleLike(postId);
        return ResultUtils.success(response);
    }

    /**
     * 4. 收藏或取消收藏一个面经 (Toggle操作)
     * @param postId 面经的唯一ID
     * @return 操作后的收藏状态和最新的收藏总数
     */
    @PostMapping("/collect")
    public BaseResponse<InteractionToggleResponse> toggleCollect(@PathVariable Long postId) {
        InteractionToggleResponse response = experienceCommentService.toggleCollect(postId);
        return ResultUtils.success(response);
    }
}
