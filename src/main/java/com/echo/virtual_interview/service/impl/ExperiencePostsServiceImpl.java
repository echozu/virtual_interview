package com.echo.virtual_interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.mapper.*;
import com.echo.virtual_interview.model.dto.analysis.ChartDataDTO;
import com.echo.virtual_interview.model.dto.analysis.InterviewReportResponseDTO;
import com.echo.virtual_interview.model.dto.analysis.RadarDataDTO;
import com.echo.virtual_interview.model.dto.experience.*;
import com.echo.virtual_interview.model.entity.*;
import com.echo.virtual_interview.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
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


    @Resource
    private ExperienceInteractionsMapper interactionMapper;
    @Resource
    private ExperiencePostsMapper experiencePostMapper;
    @Resource
    private InterviewGeneratedReportsMapper reportMapper;

    @Resource
    private UsersMapper userMapper;
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

        // 步骤 2: 构造实体并拷贝基本属性
        ExperiencePosts post = new ExperiencePosts();
        BeanUtils.copyProperties(createRequest, post);
        post.setUserId(Long.valueOf(userId)); // 设置作者ID
        // 设置初始状态，建议使用枚举
        post.setStatus("PUBLISHED");

        // 步骤 3: 处理标签列表 -> 转换为JSON字符串
        List<String> tagsList = createRequest.getTags();
        if (tagsList != null && !tagsList.isEmpty()) {
            try {
                String tagsJson = objectMapper.writeValueAsString(tagsList);
                post.setTags(tagsJson);
            } catch (JsonProcessingException e) {
                log.error("标签列表序列化为JSON时出错, tags: {}", tagsList, e);
                throw new BusinessException(500, "系统错误，处理标签失败");
            }
        }

        // 步骤 4: 处理 sharedReportElements -> 转换为JSON字符串
        Object sharedElements = createRequest.getSharedReportElements();
        if (sharedElements != null) {
            try {
                // 不论 sharedElements 是 Map 还是其他对象，都将其序列化为 JSON 字符串
                String sharedElementsJson = objectMapper.writeValueAsString(sharedElements);
                post.setSharedReportElements(sharedElementsJson);
            } catch (JsonProcessingException e) {
                log.error("分享报告元素序列化为JSON时出错, elements: {}", sharedElements, e);
                throw new BusinessException(500, "系统错误，处理分享设置失败");
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
    /**
     * 获取可分享的、可展示/隐藏的报告模块列表
     * @return 可分享模块的列表
     */
    public List<ShareableElementDTO> getShareableElements() {
        List<ShareableElementDTO> elements = new ArrayList<>();

        // --- 核心模块 (默认分享) ---
        elements.add(new ShareableElementDTO("overall_summary", "总体表现概述", true));
        elements.add(new ShareableElementDTO("radar_data", "能力雷达图", true));
        elements.add(new ShareableElementDTO("behavioral_analysis", "行为表现分析", true));
        elements.add(new ShareableElementDTO("overall_suggestions", "综合改进建议", true));
        elements.add(new ShareableElementDTO("behavioral_suggestions", "行为细节建议", true));
        elements.add(new ShareableElementDTO("recommendations", "个性化学习推荐", true));

        // --- 敏感/详细模块 (默认不分享) ---
        elements.add(new ShareableElementDTO("question_analysis_data", "逐题分析详情", false));
        elements.add(new ShareableElementDTO("tension_curve_data", "紧张度曲线", false));
        elements.add(new ShareableElementDTO("filler_word_usage", "口头禅使用分析", false));
        elements.add(new ShareableElementDTO("eye_contact_percentage", "眼神交流分析", false));

        // position, interviewDate, interviewType 等作为基础信息，始终展示，不作为可选项

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
    @Transactional // 包含写操作（浏览量+1），建议开启事务
    @SneakyThrows // 使用 Lombok 简化受检异常（如JSON处理异常）的书写
    public ExperiencePostDetailResponse getExperiencePostDetail(Long postId) {
        // 步骤 0: 获取当前查看者ID（如果未登录，则为null）
        Integer viewerId = UserIdContext.getUserIdContext();

        // 步骤 1: 获取面经主帖信息
        ExperiencePosts post = experiencePostMapper.selectById(postId);
        if (post == null || !"PUBLISHED".equals(post.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "面经不存在或未发布");
        }

        // 步骤 2: 增加浏览量（这里使用简化同步实现）
        post.setViewsCount(post.getViewsCount() + 1);
        experiencePostMapper.updateById(post);

        // 步骤 3: 获取关联数据
        InterviewGeneratedReports report = reportMapper.selectOne(new QueryWrapper<InterviewGeneratedReports>().eq("session_id", post.getSessionId()));
        if (report == null) throw new BusinessException(ErrorCode.SYSTEM_ERROR, "关联的面试报告丢失");

        Users author = userMapper.selectById(post.getUserId());
        if (author == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作者信息不存在");

        // 步骤 4: 根据分享设置，构建筛选后的AI报告
        SharedAiReportDTO filteredAiReport = buildSharedAiReport(post.getSharedReportElements(), report.getReportData());

        // 步骤 5: 检查当前查看者的互动状态（是否点赞/收藏）
        boolean isLiked = false;
        boolean isCollected = false;
        if (viewerId != null) {
            isLiked = interactionMapper.exists(new QueryWrapper<ExperienceInteractions>()
                    .eq("post_id", postId)
                    .eq("user_id", viewerId)
                    .eq("interaction_type", "LIKE"));
            isCollected = interactionMapper.exists(new QueryWrapper<ExperienceInteractions>()
                    .eq("post_id", postId)
                    .eq("user_id", viewerId)
                    .eq("interaction_type", "COLLECT"));
        }

        // 步骤 6: 组装并返回最终的响应DTO
        return buildDetailResponse(post, author, filteredAiReport, isLiked, isCollected);
    }

    /**
     * 从所有获取和处理过的数据中，组装最终的响应DTO。
     */
    @SneakyThrows
    private ExperiencePostDetailResponse buildDetailResponse(ExperiencePosts post, Users author, SharedAiReportDTO filteredAiReport, boolean isLiked, boolean isCollected) {

        // 处理作者信息（区分匿名和非匿名）
        ExperiencePostDetailResponse.AuthorInfoDTO authorInfo = post.getIsAnonymous()
                ? ExperiencePostDetailResponse.AuthorInfoDTO.builder().nickname("匿名用户").avatar(null).build()
                : ExperiencePostDetailResponse.AuthorInfoDTO.builder().userId(author.getId()).nickname(author.getNickname()).avatar(author.getAvatarUrl()).build();

        // 构建帖子统计数据
        ExperiencePostDetailResponse.PostStatsDTO stats = ExperiencePostDetailResponse.PostStatsDTO.builder()
                .viewsCount(post.getViewsCount())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .collectionsCount(post.getCollectionsCount())
                .build();

        // 构建当前用户的互动状态
        ExperiencePostDetailResponse.ViewerInteractionDTO viewerInteraction = ExperiencePostDetailResponse.ViewerInteractionDTO.builder()
                .isLiked(isLiked)
                .isCollected(isCollected)
                .build();

        // 解析标签JSON字符串为列表
        List<String> tags = post.getTags() != null ? objectMapper.readValue(post.getTags(), new TypeReference<>() {}) : Collections.emptyList();

        // 使用Builder模式构建最终响应对象
        return ExperiencePostDetailResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .experienceUrl(post.getExperienceUrl())
                .createdAt(post.getCreatedAt())
                .tags(tags)
                .authorInfo(authorInfo)
                .stats(stats)
                .viewerInteraction(viewerInteraction)
                .aiReport(filteredAiReport)
                .build();
    }

    /**
     * AI报告的核心筛选逻辑。
     * 读取完整报告，然后根据分享设置，将不分享的模块置为null。
     */
    @SneakyThrows
    private SharedAiReportDTO buildSharedAiReport(String sharedElementsJson, String fullReportJson) {
        // 如果没有分享设置（老数据兼容），则默认全部分享（除了敏感信息）
        if (sharedElementsJson == null) {
            return objectMapper.readValue(fullReportJson, SharedAiReportDTO.class);
        }

        // 解析用户的分享设置
        SharedReportElementsDTO settings = objectMapper.readValue(sharedElementsJson, SharedReportElementsDTO.class);
        // 解析完整的报告数据
        SharedAiReportDTO fullReport = objectMapper.readValue(fullReportJson, SharedAiReportDTO.class);

        // 核心逻辑：将未分享（设置为false）的字段置为null
        // Jackson在序列化时会自动忽略null值
        if (!settings.isOverallSummary()) fullReport.setOverallSummary(null);
        if (!settings.isRadarData()) fullReport.setRadarData(null);
        if (!settings.isBehavioralAnalysis()) fullReport.setBehavioralAnalysis(null);
        if (!settings.isOverallSuggestions()) fullReport.setOverallSuggestions(null);
        if (!settings.isBehavioralSuggestions()) fullReport.setBehavioralSuggestions(null);
        if (!settings.isQuestionAnalysisData()) fullReport.setQuestionAnalysisData(null);
        if (!settings.isTensionCurveData()) fullReport.setTensionCurveData(null);
        if (!settings.isFillerWordUsage()) fullReport.setFillerWordUsage(null);
        if (!settings.isEyeContactPercentage()) fullReport.setEyeContactPercentage(null);
        if (!settings.isRecommendations()) fullReport.setRecommendations(null);

        return fullReport;
    }

    @Resource
    private LeaderboardMapper leaderboardMapper;

    private static final int LEADERBOARD_LIMIT = 4; // 排行榜显示数量

    @Override
    public LeaderboardResponse getLeaderboards() {
        // 1. 获取本周热门帖子的原始数据
        LocalDateTime startOfWeek = LocalDateTime.now().with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay();
        List<Map<String, Object>> rawWeeklyHotPosts = leaderboardMapper.findTopLikedPostsInPeriod(startOfWeek, LEADERBOARD_LIMIT);

        // 手动将 List<Map> 转换为 List<HotPostDTO>，并处理tags字段
        List<HotPostDTO> weeklyHotPosts = rawWeeklyHotPosts.stream().map(rawPost -> {
            HotPostDTO dto = new HotPostDTO();
            // 注意：从Map中取值时，MyBatis可能会根据数据库驱动返回Long或BigInteger，强转为Number再取longValue更安全
            dto.setPostId(((Number) rawPost.get("postId")).longValue());
            dto.setTitle((String) rawPost.get("title"));
            dto.setExperienceUrl((String) rawPost.get("experienceUrl"));

            String tagsJson = (String) rawPost.get("tags");
            if (StringUtils.isNotBlank(tagsJson)) {
                try {
                    dto.setTags(objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {}));
                } catch (JsonProcessingException e) {
                    log.warn("解析周热门榜帖子标签失败, postId: {}, tags: {}", dto.getPostId(), tagsJson, e);
                    dto.setTags(Collections.emptyList());
                }
            } else {
                dto.setTags(Collections.emptyList());
            }
            return dto;
        }).collect(Collectors.toList());

        // 2. 获取本月获赞最多的4位用户
        LocalDateTime startOfMonth = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();
        List<TopAuthorDTO> monthlyTopAuthors = leaderboardMapper.findTopLikedAuthorsInPeriod(startOfMonth, LEADERBOARD_LIMIT);

        // 3. 获取评论最多的4篇面经
        QueryWrapper<ExperiencePosts> queryWrapper = new QueryWrapper<>();
        // 在查询中加入 experience_url 和 tags 字段
        queryWrapper.select("id", "title", "experience_url", "tags")
                .eq("status", "PUBLISHED")
                .eq("visibility", "PUBLIC")
                .orderByDesc("comments_count")
                .last("LIMIT " + LEADERBOARD_LIMIT);

        List<ExperiencePosts> topCommentedPosts = experiencePostMapper.selectList(queryWrapper);

        // 使用 .stream().map() 进行转换
        List<HotPostDTO> mostCommentedPosts = topCommentedPosts.stream()
                .map(post -> {
                    HotPostDTO dto = new HotPostDTO();
                    dto.setPostId(post.getId());
                    dto.setTitle(post.getTitle());
                    // 直接设置 experienceUrl
                    dto.setExperienceUrl(post.getExperienceUrl());

                    // ==================== 核心修改开始 ====================
                    // 处理 tags 字段，从JSON字符串转换为List<String>
                    if (StringUtils.isNotBlank(post.getTags())) {
                        try {
                            // 使用 ObjectMapper 将JSON数组字符串反序列化为List
                            List<String> tagList = objectMapper.readValue(post.getTags(), new TypeReference<List<String>>() {});
                            dto.setTags(tagList);
                        } catch (JsonProcessingException e) {
                            // 如果解析失败，打印警告日志，并设置一个空列表以避免前端出错
                            log.warn("解析面经标签失败, postId: {}, tags: {}", post.getId(), post.getTags(), e);
                            dto.setTags(Collections.emptyList());
                        }
                    } else {
                        // 如果tags字段本身为空，也设置一个空列表
                        dto.setTags(Collections.emptyList());
                    }
                    // ==================== 核心修改结束 ====================

                    return dto;
                }).collect(Collectors.toList());


        // 4. 组装响应对象
        return LeaderboardResponse.builder()
                .weeklyHotPosts(weeklyHotPosts)
                .monthlyTopAuthors(monthlyTopAuthors)
                .mostCommentedPosts(mostCommentedPosts)
                .build();
    }

    @Override
    @SneakyThrows
    @Cacheable(cacheNames = "ExperienceFilterOptions")
    public Map<String, Object> getExperienceFilterOptions() {
        // 使用 LinkedHashMap 来保证前端展示的顺序
        Map<String, Object> filterMap = new LinkedHashMap<>();

        // 1. 定义固定的筛选分类
        List<String> jobTypes = Arrays.asList("校招", "社招", "实习", "兼职", "不限");
        // 建议：未来 positions 和 companies 也可以从 interview_channels 表中动态获取
        List<String> positions = Arrays.asList("后端工程师", "前端工程师", "算法工程师", "产品经理", "数据分析师");
        List<String> companies = Arrays.asList("阿里巴巴", "腾讯", "字节跳动", "华为", "美团");
        List<String> sortOptions = Arrays.asList("hot", "latest", "mostCollected");

        // ==================== 动态获取标签的核心逻辑 开始 ====================

        // 2.1 从数据库查询所有帖子的tags JSON字符串列表
        List<String> allTagsJsonList = experiencePostMapper.selectAllTagsJson();

        // 2.2 在Java内存中解析、聚合、统计频率
        // 将所有JSON数组字符串扁平化为一个包含所有标签的单一列表
        List<String> flattenedTags = new ArrayList<>();
        for (String tagsJson : allTagsJsonList) {
            List<String> tags = objectMapper.readValue(tagsJson, new TypeReference<>() {});
            flattenedTags.addAll(tags);
        }

        // 2.3 使用Stream API统计每个标签的出现次数
        Map<String, Long> tagFrequencies = flattenedTags.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // 2.4 按频率降序排序，并提取标签名，最多返回前30个热门标签
        List<String> popularTags = tagFrequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(30)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // ==================== 动态获取标签的核心逻辑 结束 ====================

        // 3. 将中文键和对应的列表放入 Map
        filterMap.put("工作类型", jobTypes);
        filterMap.put("应聘岗位", positions);
        filterMap.put("热门公司", companies);
        filterMap.put("热门标签", popularTags); // 使用动态获取的热门标签
        filterMap.put("排序方式", sortOptions);

        return filterMap;
    }

    @Override
    @SneakyThrows
    public Page<ExperiencePostVO> listExperiencePostsByPage(ExperiencePostQueryRequest queryRequest) {
        // 1. 创建MyBatis-Plus分页对象
        Page<ExperiencePostVO> page = new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize());

        // 2. 调用Mapper的自定义方法执行查询
        // MyBatis-Plus分页插件会自动拦截这个方法，并执行分页
        IPage<ExperiencePostVO> voPage = baseMapper.selectPostVOPage(page, queryRequest);

        // 3. 对查询结果进行后处理 (主要是转换tags)
        List<ExperiencePostVO> records = voPage.getRecords();
        for (ExperiencePostVO vo : records) {
            // 如果是匿名，将昵称设为"匿名用户"
            if (vo.getIsAnonymous()) {
                vo.setAuthorNickname("匿名用户");
            }

            // 将tags的JSON字符串转换为List<String>
            if (StringUtils.isNotBlank(vo.getTagsJson())) {
                vo.setTags(objectMapper.readValue(vo.getTagsJson(), new TypeReference<>() {}));
            } else {
                vo.setTags(Collections.emptyList());
            }
        }

        // 4. 类型转换后返回 (IPage就是Page)
        return (Page<ExperiencePostVO>) voPage;
    }

    @Resource
    private ExperienceCommentsMapper experienceCommentMapper;

    @Resource
    private ExperienceInteractionsMapper experienceInteractionMapper;
    @Override
    @Transactional // @Transactional 注解确保所有删除操作要么全部成功，要么全部失败回滚
    public void deleteExperiencePost(Long postId) {
        // 1. 获取当前登录用户ID
        Integer userId = UserIdContext.getUserIdContext();
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 2. 查询待删除的面经
        ExperiencePosts post = this.getById(postId);
        if (post == null) {
            // 如果帖子不存在，可以直接返回或抛出异常，这里选择不抛异常，操作幂等
            log.warn("尝试删除不存在的面经，postId: {}", postId);
            return;
        }

        // 3. 权限校验：只有作者本人或管理员可以删除
        // TODO: 未来可以实现一个 isAdmin(userId) 的方法来判断管理员权限
        if (!Objects.equals(post.getUserId(), Long.valueOf(userId)) /* && !isAdmin(userId) */) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该面经");
        }

        // 4. 删除主表记录
        boolean removed = this.removeById(postId);
        if (removed) {
            log.info("用户 {} 删除了面经，ID: {}", userId, postId);

            // 5. 手动级联删除关联数据
            // 5.1 删除该面经下的所有评论
            int deletedComments = experienceCommentMapper.delete(new QueryWrapper<ExperienceComments>().eq("post_id", postId));
            if (deletedComments > 0) {
                log.info("级联删除了 {} 条评论，postId: {}", deletedComments, postId);
            }

            // 5.2 删除该面经下的所有互动记录（点赞、收藏）
            int deletedInteractions = experienceInteractionMapper.delete(new QueryWrapper<ExperienceInteractions>().eq("post_id", postId));
            if (deletedInteractions > 0) {
                log.info("级联删除了 {} 条互动记录，postId: {}", deletedInteractions, postId);
            }

        } else {
            // 正常情况下，如果权限校验通过，这里不应该执行到。但为了健壮性，加上日志。
            log.error("删除面经主表记录失败，postId: {}", postId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败，请稍后重试");
        }
    }

}
