package com.echo.virtual_interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.mapper.InterviewSessionsMapper;
import com.echo.virtual_interview.model.dto.analysis.ChartDataDTO;
import com.echo.virtual_interview.model.dto.analysis.InterviewReportResponseDTO;
import com.echo.virtual_interview.model.dto.analysis.RadarDataDTO;
import com.echo.virtual_interview.model.dto.experience.*;
import com.echo.virtual_interview.model.entity.*;
import com.echo.virtual_interview.mapper.ExperiencePostsMapper;
import com.echo.virtual_interview.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <p>
 * 面经分享主表 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
/**
 * 面经分享 服务实现类
 */
@Service
@Slf4j
public class ExperiencePostsServiceImpl extends ServiceImpl<ExperiencePostsMapper, ExperiencePosts> implements IExperiencePostsService {
    @Resource
    private ObjectMapper objectMapper; // 注入Jackson的ObjectMapper用于JSON操作
    @Resource
    private InterviewSessionsMapper interviewSessionMapper;

    // --- 注入所有需要的服务 ---
    @Resource
    private IUsersService userService;
    @Resource
    private IInterviewService interviewService;
    @Resource
    private IInterviewChannelsService interviewChannelService;
    @Resource
    private IAnalysisReportsService analysisReportService;
    @Resource
    private IExperienceInteractionsService interactionService;
    @Resource
    private IInterviewSessionsService interviewSessionsService;
    @Resource
    private IInterviewGeneratedReportsService interviewGeneratedReports;
    @Override
    @Transactional // 开启事务，保证数据一致性
    public Long createExperiencePost(ExperiencePostCreateRequest createRequest) {
        // 步骤 0: 获取当前登录用户ID
        Integer userId = UserIdContext.getUserIdContext();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 步骤 1: 参数校验
        if (createRequest == null) {
            throw new BusinessException(400, "请求参数不能为空");
        }
        if (StringUtils.isBlank(createRequest.getTitle()) || StringUtils.isBlank(createRequest.getContent())) {
            throw new BusinessException(400, "标题和内容不能为空");
        }
        if (StringUtils.isBlank(createRequest.getSessionId())) {
            throw new BusinessException(400, "关联的面试ID不能为空");
        }

//        // 步骤 2: 业务校验：一次面试会话只能创建一篇面经
//        long count = this.count(new QueryWrapper<ExperiencePosts>().eq("session_id", createRequest.getSessionId()));
//        if (count > 0) {
//            throw new BusinessException(500, "该面试已分享过面经，请勿重复创建");
//        }

        // 步骤 3: 构造实体并拷贝属性
        ExperiencePosts post = new ExperiencePosts();
        BeanUtils.copyProperties(createRequest, post);
        post.setUserId(Long.valueOf(userId)); // 设置作者ID

        // 步骤 4: 处理标签列表 -> 转换为JSON字符串
        List<String> tagsList = createRequest.getTags();
        if (tagsList != null && !tagsList.isEmpty()) {
            try {
                // 使用ObjectMapper将List<String>序列化为JSON数组字符串
                String tagsJson = objectMapper.writeValueAsString(tagsList);
                post.setTags(tagsJson);
            } catch (JsonProcessingException e) {
                log.error("标签列表序列化为JSON时出错, tags: {}", tagsList, e);
                throw new BusinessException(500, "系统错误，处理标签失败");
            }
        }

        // 步骤 5: 保存主表数据到数据库
        boolean isSuccess = this.save(post);
        if (!isSuccess) {
            throw new BusinessException(500, "创建面经失败，请稍后重试");
        }

        log.info("用户 {} 为会话 {} 创建了新的面经，ID: {}", userId, post.getSessionId(), post.getId());

        // 步骤 6: 返回新创建的面经ID
        return post.getId();
    }
    /**
     * 获取所有可供用户选择分享的AI报告模块列表
     * <p>
     * 这是一个固定的列表，直接在代码中定义。
     * @return 模块列表
     */
    @Override
    public List<ShareableElementDTO> getShareableElements() {
        List<ShareableElementDTO> elements = new ArrayList<>();

        // 从您的 InterviewReportResponseDTO 中提取可分享的模块
        elements.add(new ShareableElementDTO("overall_summary", "总体表现概述", true));
        elements.add(new ShareableElementDTO("radar_data", "能力雷达图", true));
        elements.add(new ShareableElementDTO("behavioral_analysis", "行为表现分析", true));
        elements.add(new ShareableElementDTO("overall_suggestions", "综合改进建议", true));
        elements.add(new ShareableElementDTO("question_analysis_data", "逐题分析详情", false)); // 默认不分享，因为信息太详细
        elements.add(new ShareableElementDTO("tension_curve_data", "紧张度曲线", false)); // 默认不分享，比较敏感
        elements.add(new ShareableElementDTO("filler_word_usage", "口头禅使用分析", false)); // 默认不分享，比较敏感
        elements.add(new ShareableElementDTO("recommendations", "个性化学习推荐", true));
        // dialogues（完整对话记录）通常过于敏感，一般不作为可选项

        return elements;
    }
    /**
     * 获取指定用户的历史面试记录列表（用于选择并发表面经）
     * <p>
     * 此方法现在位于 ExperiencePostServiceImpl 中。
     *
     * @param userId 用户ID
     * @return 包含处理后信息的DTO列表
     */
    @Override
    public List<InterviewHistoryDTO> getHistoryWithExperience(Integer userId) {
        // 步骤 1: 调用注入的 interviewSessionMapper 获取基础历史记录列表
        // 注意：这里不再是 this.baseMapper，而是专门注入的 mapper
        List<InterviewHistoryDTO> historyList = interviewSessionMapper.listHistoryWithExperience(userId);

        // 如果列表为空，直接返回，无需后续处理
        if (CollectionUtils.isEmpty(historyList)) {
            return Collections.emptyList();
        }

        // 步骤 2: 批量检查哪些面试已经发表过面经
        // 2.1 提取所有sessionId
        List<String> sessionIds = historyList.stream()
                .map(InterviewHistoryDTO::getSessionId)
                .collect(Collectors.toList());

        // 2.2 在本 Service 中查询 experience_posts 表中已存在的 sessionId
        // 注意：这里可以直接使用 this.list()，因为当前就是在 ExperiencePostService 中
        QueryWrapper<ExperiencePosts> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("session_id", sessionIds);
        List<ExperiencePosts> existingPosts = this.list(queryWrapper);

        // 2.3 将已存在的sessionId存入Set，便于快速查找
        Set<String> existingSessionIds = existingPosts.stream()
                .map(ExperiencePosts::getSessionId)
                .collect(Collectors.toSet());

        // 步骤 3: 遍历并完善DTO列表
        historyList.forEach(dto -> {
            // 3.1 设置hasExperiencePost标志
            dto.setHasExperiencePost(existingSessionIds.contains(dto.getSessionId()));

            // 3.2 处理标题为空的情况（例如快速面试）
            if (StringUtils.isBlank(dto.getInterviewTitle())) {
                dto.setInterviewTitle("快速面试");
            }
        });

        return historyList;
    }
    @Override
    public ExperiencePostDetailResponse getExperiencePostDetail(Long postId) {
        // --- 步骤 1: 查询面经主表信息 ---
        ExperiencePosts post = this.getById(postId);
        if (post == null || !"PUBLISHED".equals(post.getStatus())) {
            throw new BusinessException(404, "面经不存在或未发布");
        }

        // --- 步骤 2: 异步更新浏览量 ---
        // 为了提升接口响应速度，将写操作异步化
        this.incrementViewsCount(postId);

        // --- 步骤 3: 初始化响应DTO ---
        ExperiencePostDetailResponse detailResponse = new ExperiencePostDetailResponse();
        BeanUtils.copyProperties(post, detailResponse);

        // --- 步骤 4: 组装聚合信息 ---
        // 使用 CompletableFuture 并行执行多个不相关的查询，提升性能
        Integer currentUserId = UserIdContext.getUserIdContext();

        CompletableFuture<Void> authorFuture = CompletableFuture.runAsync(() ->
                assembleAuthorInfo(detailResponse, post));

        CompletableFuture<Void> tagsFuture = CompletableFuture.runAsync(() ->
                assembleTags(detailResponse, post));

        CompletableFuture<Void> interactionFuture = CompletableFuture.runAsync(() ->
                assembleInteractionStatus(detailResponse, postId, currentUserId));

        CompletableFuture<Void> reportAndChannelFuture = CompletableFuture.runAsync(() ->
                assembleReportAndChannelInfo(detailResponse, post));

        // 等待所有异步任务完成
        CompletableFuture.allOf(authorFuture, tagsFuture, interactionFuture, reportAndChannelFuture).join();

        return detailResponse;
    }

    /**
     * 异步增加浏览量
     */
    @Async
    public void incrementViewsCount(Long postId) {
        // 使用SQL更新，避免先读后写可能产生的并发问题
        this.update().setSql("views_count = views_count + 1").eq("id", postId).update();
    }

    // --- 以下为组装各个部分的私有方法 ---

    private void assembleAuthorInfo(ExperiencePostDetailResponse detailResponse, ExperiencePosts post) {
        Users author = userService.getById(post.getUserId());
        if (author != null) {
            ExperiencePostDetailResponse.AuthorInfoVO authorInfo = new ExperiencePostDetailResponse.AuthorInfoVO();
            if (Boolean.TRUE.equals(post.getIsAnonymous())) {
                authorInfo.setAuthorId(author.getId().intValue());
                authorInfo.setNickname("匿名用户");
                // 匿名用户可以不显示头像或使用默认头像
                authorInfo.setAvatarUrl(null);
            } else {
                authorInfo.setAuthorId(author.getId().intValue());
                authorInfo.setNickname(author.getNickname());
                authorInfo.setAvatarUrl(author.getAvatarUrl());
            }
            detailResponse.setAuthorInfo(authorInfo);
        }
    }

    private void assembleTags(ExperiencePostDetailResponse detailResponse, ExperiencePosts post) {
        String tagsJson = post.getTags();
        if (StringUtils.isNotBlank(tagsJson)) {
            try {
                List<String> tags = objectMapper.readValue(tagsJson, new TypeReference<>() {});
                detailResponse.setTags(tags);
            } catch (JsonProcessingException e) {
                log.error("解析面经标签JSON失败, postId: {}, tagsJson: {}", post.getId(), tagsJson, e);
                detailResponse.setTags(Collections.emptyList());
            }
        }
    }

    private void assembleInteractionStatus(ExperiencePostDetailResponse detailResponse, Long postId, Integer currentUserId) {
        if (currentUserId == null) {
            detailResponse.setIsLiked(false);
            detailResponse.setIsCollected(false);
            return;
        }
        long likeCount = interactionService.count(new QueryWrapper<ExperienceInteractions>()
                .eq("post_id", postId)
                .eq("user_id", currentUserId)
                .eq("interaction_type", "LIKE"));
        long collectCount = interactionService.count(new QueryWrapper<ExperienceInteractions>()
                .eq("post_id", postId)
                .eq("user_id", currentUserId)
                .eq("interaction_type", "COLLECT"));
        detailResponse.setIsLiked(likeCount > 0);
        detailResponse.setIsCollected(collectCount > 0);
    }

    /**
     * 组装报告详情中的 "AI报告摘要" 和 "面试频道信息" 部分
     * @param detailResponse 待填充的响应DTO
     * @param post 当前的面经实体
     *             1.直接把分析报告组装出去 但是根据true或false将一些字段设置为false即可
     */
    private void assembleReportAndChannelInfo(ExperiencePostDetailResponse detailResponse, ExperiencePosts post) {
        // 1. 获取面试会话和频道信息 (此部分逻辑正确，保持不变)
        InterviewSessions session = interviewSessionsService.getOne(new LambdaQueryWrapper<InterviewSessions>().eq(InterviewSessions::getId, post.getSessionId()));
        if (session != null && session.getChannelId() != null) {
            InterviewChannels channel = interviewChannelService.getById(session.getChannelId());
            if (channel != null) {
                ExperiencePostDetailResponse.InterviewChannelInfoVO channelInfo = new ExperiencePostDetailResponse.InterviewChannelInfoVO();
                channelInfo.setChannelId(channel.getId());
                channelInfo.setTitle(channel.getTitle());
                detailResponse.setChannelInfo(channelInfo);
            }
        }

        // 2. 从面经实体中获取用户设置的分享偏好 (此部分逻辑正确，保持不变)
        // shared_report_elements 字段存储了类似 {"radar_data": true, "overall_summary": false} 的JSON
        Map<String, Boolean> sharingPrefs = Collections.emptyMap();
        if (post.getSharedReportElements() != null) {
            try {
                sharingPrefs = objectMapper.convertValue(post.getSharedReportElements(), new TypeReference<>() {});
            } catch (IllegalArgumentException e) {
                log.error("解析分享偏好JSON失败, postId: {}", post.getId(), e);
            }
        }

        // 3. 如果用户没有选择任何分享项，则提前结束
        if (sharingPrefs.isEmpty() || sharingPrefs.values().stream().noneMatch(Boolean.TRUE::equals)) {
            return;
        }

        // 4. 从数据库获取完整的持久化报告 (此部分逻辑正确，保持不变)
        InterviewGeneratedReports generatedReportEntity = interviewGeneratedReports.getOne(
                new LambdaQueryWrapper<InterviewGeneratedReports>().eq(InterviewGeneratedReports::getSessionId, post.getSessionId())
        );

        if (generatedReportEntity == null || StringUtils.isBlank(generatedReportEntity.getReportData())) {
            log.warn("未找到或报告数据为空，无法为面经 postId: {} 组装AI摘要", post.getId());
            return;
        }

        // 5. 根据分享偏好，选择性地从完整报告中提取数据并填充
        try {
            // 反序列化完整的报告JSON
            InterviewReportResponseDTO fullReport = objectMapper.readValue(generatedReportEntity.getReportData(), InterviewReportResponseDTO.class);

            // 创建用于返回的摘要对象
            ExperiencePostDetailResponse.AiReportSummaryVO summaryVO = new ExperiencePostDetailResponse.AiReportSummaryVO();
            summaryVO.setSessionId(post.getSessionId());

            // --- 核心逻辑：遍历检查每一项分享设置 ---

            // 检查 "overall_summary" 是否被设置为 true
            if (sharingPrefs.getOrDefault("overall_summary", false)) {
                summaryVO.setHighlights(fullReport.getOverall_summary());
            }

            // 检查 "radar_data" 是否被设置为 true
            if (sharingPrefs.getOrDefault("radar_data", false)) {
                if (fullReport.getRadar_data() != null) {
                    Map<String, Object> radarMap = objectMapper.convertValue(fullReport.getRadar_data(), new TypeReference<>() {});
                    summaryVO.setRadarChartData(radarMap);
                }
            }

            // 检查 "overall_suggestions" 是否被设置为 true
            if (sharingPrefs.getOrDefault("overall_suggestions", false)) {
                summaryVO.setSuggestions(fullReport.getOverall_suggestions());
            }

            // 检查 "overall_score" 是否被设置为 true
            if (sharingPrefs.getOrDefault("overall_score", false) && session != null) {
                summaryVO.setOverallScore(session.getOverallScore());
            }

            // --- 将填充好的摘要对象设置到最终的响应中 ---
            detailResponse.setAiReportSummary(summaryVO);

        } catch (JsonProcessingException e) {
            log.error("反序列化已存储的报告JSON失败, sessionId: {}", post.getSessionId(), e);
        }
    }

    @Override
    public Page<ExperiencePostVO> listExperiencePostsByPage(ExperiencePostQueryRequest queryRequest) {
        Page<ExperiencePostVO> page = new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize());
        // 调用Mapper中的自定义XML查询
        return this.baseMapper.listExperiencePostVO(page, queryRequest);
    }

    @Override
    @Transactional
    public void deleteExperiencePost(Long postId) {
        Integer userId = UserIdContext.getUserIdContext();
        ExperiencePosts post = this.getById(postId);

        // 权限校验：只有作者本人或管理员可以删除
        if (post == null || !Objects.equals(post.getUserId(), userId) /* && !isAdmin(userId) */) {
            throw new BusinessException(500,"无权删除该面经");
        }

        // 1. 删除主表记录
        boolean removed = this.removeById(postId);
        if (removed) {
            log.info("用户 {} 删除了面经，ID: {}", userId, postId);
            // 2. TODO: 手动级联删除
            //    - 删除 experience_post_tags 表中所有 post_id 为 postId 的记录
            //    - 删除 experience_comments 表中所有 post_id 为 postId 的记录
            //    - 删除 experience_interactions 表中所有 post_id 为 postId 的记录
        }
    }

}
