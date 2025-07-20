package com.echo.virtual_interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.mapper.ExperienceInteractionsMapper;
import com.echo.virtual_interview.mapper.ExperiencePostsMapper;
import com.echo.virtual_interview.mapper.UsersMapper;
import com.echo.virtual_interview.model.dto.comment.CommentCreateRequest;
import com.echo.virtual_interview.model.dto.comment.CommentVO;
import com.echo.virtual_interview.model.dto.comment.InteractionToggleResponse;
import com.echo.virtual_interview.model.entity.ExperienceComments;
import com.echo.virtual_interview.mapper.ExperienceCommentsMapper;
import com.echo.virtual_interview.model.entity.ExperienceInteractions;
import com.echo.virtual_interview.model.entity.ExperiencePosts;
import com.echo.virtual_interview.model.entity.Users;
import com.echo.virtual_interview.service.IExperienceCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 面经评论表 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
@Service
@Slf4j
public class ExperienceCommentsServiceImpl extends ServiceImpl<ExperienceCommentsMapper, ExperienceComments> implements IExperienceCommentsService {

    @Resource
    private ExperiencePostsMapper experiencePostMapper;
    @Resource
    private ExperienceInteractionsMapper experienceInteractionMapper;
    @Resource
    private UsersMapper userMapper;

    /**
     * 获取评论树（核心方法）
     */
    @Override
    public Page<CommentVO> getCommentTreePage(Long postId, long current, long pageSize) {
        // 1. 分页查询顶级评论 (parent_comment_id is null)
        Page<ExperienceComments> page = new Page<>(current, pageSize);
        QueryWrapper<ExperienceComments> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("post_id", postId)
                .isNull("parent_comment_id")
                .orderByDesc("created_at");
        Page<ExperienceComments> topLevelCommentPage = this.page(page, queryWrapper);

        List<ExperienceComments> topLevelComments = topLevelCommentPage.getRecords();
        if (topLevelComments.isEmpty()) {
            return new Page<>(current, pageSize, topLevelCommentPage.getTotal());
        }

        // 2. 获取所有顶级评论的ID
        List<Long> topLevelCommentIds = topLevelComments.stream()
                .map(ExperienceComments::getId)
                .collect(Collectors.toList());

        // 3. 一次性查询出所有顶级评论下的所有回复
        List<ExperienceComments> replies = this.list(new QueryWrapper<ExperienceComments>()
                .in("parent_comment_id", topLevelCommentIds)
                .orderByAsc("created_at"));

        // 4. 高效组装：一次性查询所有评论者（包括顶级评论和回复）的用户信息
        Set<Long> userIds = topLevelComments.stream().map(ExperienceComments::getUserId).collect(Collectors.toSet());
        userIds.addAll(replies.stream().map(ExperienceComments::getUserId).collect(Collectors.toSet()));
        Map<Long, Users> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(Users::getId, Users -> Users));

        // 5. 将回复按 parent_comment_id 分组
        Map<Long, List<CommentVO>> repliesMap = replies.stream()
                .map(comment -> convertToVO(comment, userMap.get(comment.getUserId())))
                .collect(Collectors.groupingBy(vo -> findParentId(vo, replies))); // 自定义查找父ID的方法，因为VO中没有parent_id

        // 6. 组装最终的VO树
        List<CommentVO> commentTreeVOs = topLevelComments.stream().map(comment -> {
            CommentVO vo = convertToVO(comment, userMap.get(comment.getUserId()));
            vo.setReplies(repliesMap.get(comment.getId()));
            return vo;
        }).collect(Collectors.toList());

        // 7. 构建并返回VO分页结果
        Page<CommentVO> voPage = new Page<>(current, pageSize, topLevelCommentPage.getTotal());
        voPage.setRecords(commentTreeVOs);
        return voPage;
    }

    /**
     * 创建评论
     */
    @Override
    @Transactional
    public CommentVO createComment(Long postId, CommentCreateRequest createRequest) {
        // 1. 获取当前登录用户
        Integer userId = UserIdContext.getUserIdContext();
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 2. 校验帖子是否存在
        ExperiencePosts post = experiencePostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "面经不存在");
        }

        // 3. 创建并保存评论实体
        ExperienceComments comment = new ExperienceComments();
        comment.setPostId(postId);
        comment.setUserId(Long.valueOf(userId));
        comment.setContent(createRequest.getContent());
        comment.setParentCommentId(createRequest.getParentCommentId());
        this.save(comment);

        // 4. 原子性地更新主帖的评论数
        experiencePostMapper.update(null, new UpdateWrapper<ExperiencePosts>()
                .eq("id", postId)
                .setSql("comments_count = comments_count + 1"));

        // 5. 返回新创建的评论VO
        Users author = userMapper.selectById(userId);
        return convertToVO(comment, author);
    }

    /**
     * 切换点赞状态
     */
    @Override
    @Transactional
    public InteractionToggleResponse toggleLike(Long postId) {
        return toggleInteraction(postId, "LIKE");
    }

    /**
     * 切换收藏状态
     */
    @Override
    @Transactional
    public InteractionToggleResponse toggleCollect(Long postId) {
        return toggleInteraction(postId, "COLLECT");
    }

    // ------------------- 私有辅助方法 -------------------

    /**
     * 切换互动状态的通用私有方法，减少代码重复
     */
    private InteractionToggleResponse toggleInteraction(Long postId, String interactionType) {
        Integer userId = UserIdContext.getUserIdContext();
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 1. 检查互动记录是否存在
        QueryWrapper<ExperienceInteractions> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("post_id", postId)
                .eq("user_id", userId)
                .eq("interaction_type", interactionType);
        ExperienceInteractions interaction = experienceInteractionMapper.selectOne(queryWrapper);

        boolean isActive;
        // 2. 根据是否存在记录，执行增加或删除操作
        if (interaction != null) {
            // 已存在，则删除（取消操作）
            experienceInteractionMapper.deleteById(interaction.getId());
            isActive = false;
        } else {
            // 不存在，则新增（执行操作）
            ExperienceInteractions newInteraction = new ExperienceInteractions();
            newInteraction.setPostId(postId);
            newInteraction.setUserId(Long.valueOf(userId));
            newInteraction.setInteractionType(interactionType);
            experienceInteractionMapper.insert(newInteraction);
            isActive = true;
        }

        // 3. 原子性地更新主帖的计数
        String countField = interactionType.equalsIgnoreCase("LIKE") ? "likes_count" : "collections_count";
        String operation = isActive ? "+ 1" : "- 1";
        experiencePostMapper.update(null, new UpdateWrapper<ExperiencePosts>()
                .eq("id", postId)
                .setSql(countField + " = " + countField + " " + operation));

        // 4. 获取最新的总数并返回
        ExperiencePosts updatedPost = experiencePostMapper.selectById(postId);
        long totalCount = interactionType.equalsIgnoreCase("LIKE") ? updatedPost.getLikesCount() : updatedPost.getCollectionsCount();

        return new InteractionToggleResponse(isActive, totalCount);
    }

    /**
     * 将评论实体转换为VO
     */
    private CommentVO convertToVO(ExperienceComments comment, Users author) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);

        if (author != null) {
            CommentVO.UserInfo userInfo = new CommentVO.UserInfo();
            userInfo.setUserId(author.getId());
            userInfo.setNickname(author.getNickname());
            userInfo.setAvatarUrl(author.getAvatarUrl());
            vo.setAuthor(userInfo);
        }
        return vo;
    }

    /**
     * 辅助方法，用于在分组时找到回复对应的父评论ID
     * （这是一个变通方法，因为CommentVO本身没有parentCommentId字段）
     */
    private Long findParentId(CommentVO vo, List<ExperienceComments> allReplies) {
        return allReplies.stream()
                .filter(reply -> Objects.equals(reply.getId(), vo.getId()))
                .findFirst()
                .map(ExperienceComments::getParentCommentId)
                .orElse(null);
    }
}
