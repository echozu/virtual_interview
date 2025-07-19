package com.echo.virtual_interview.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echo.virtual_interview.model.dto.experience.*;
import com.echo.virtual_interview.model.entity.ExperiencePosts;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 面经分享主表 服务类
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
public interface IExperiencePostsService extends IService<ExperiencePosts> {
    /**
     * 创建一篇新的面经
     *
     * @param createRequest DTO包含创建所需的所有信息
     * @return 新创建的面经ID
     */
    Long createExperiencePost(ExperiencePostCreateRequest createRequest);

    /**
     * 获取面经详情
     *
     * @param postId 面经ID
     * @return 包含完整信息的面经详情DTO
     */
    ExperiencePostDetailResponse getExperiencePostDetail(Long postId);

    /**
     * 分页查询面经列表
     *
     * @param queryRequest 包含分页和筛选条件的查询DTO
     * @return 封装了VO的分页结果
     */
    Page<ExperiencePostVO> listExperiencePostsByPage(ExperiencePostQueryRequest queryRequest);

    /**
     * 删除一篇面经 (需要权限校验)
     *
     * @param postId 面经ID
     */
    void deleteExperiencePost(Long postId);

    List<InterviewHistoryDTO> getHistoryWithExperience(Integer userId);

    List<ShareableElementDTO> getShareableElements();
    /**
     * 获取所有排行榜数据
     * @return 包含多个排行榜列表的响应对象
     */
    LeaderboardResponse getLeaderboards();
}
