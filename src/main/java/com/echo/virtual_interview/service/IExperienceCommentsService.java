package com.echo.virtual_interview.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echo.virtual_interview.model.dto.comment.CommentCreateRequest;
import com.echo.virtual_interview.model.dto.comment.CommentVO;
import com.echo.virtual_interview.model.dto.comment.InteractionToggleResponse;
import com.echo.virtual_interview.model.entity.ExperienceComments;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 面经评论表 服务类
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
public interface IExperienceCommentsService extends IService<ExperienceComments> {

    /**
     * 分页获取评论树
     *
     * @param postId   面经ID
     * @param current  当前页码
     * @param pageSize 每页数量
     * @return 分页的顶级评论VO，每个VO包含其回复列表
     */
    Page<CommentVO> getCommentTreePage(Long postId, long current, long pageSize);

    /**
     * 创建一条新评论或回复
     *
     * @param postId        面经ID
     * @param createRequest 评论创建请求体
     * @return 包含作者信息的新评论VO
     */
    CommentVO createComment(Long postId, CommentCreateRequest createRequest);

    /**
     * 切换点赞状态
     *
     * @param postId 面经ID
     * @return 操作后的状态和最新的点赞总数
     */
    InteractionToggleResponse toggleLike(Long postId);

    /**
     * 切换收藏状态
     *
     * @param postId 面经ID
     * @return 操作后的状态和最新的收藏总数
     */
    InteractionToggleResponse toggleCollect(Long postId);}
