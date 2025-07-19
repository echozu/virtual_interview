package com.echo.virtual_interview.mapper;

import com.echo.virtual_interview.model.dto.experience.InterviewHistoryDTO;
import com.echo.virtual_interview.model.entity.InterviewSessions;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 记录每一次具体的面试实例 Mapper 接口
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
public interface InterviewSessionsMapper extends BaseMapper<InterviewSessions> {
    /**
     * 根据用户ID查询其简化的历史面试记录
     *
     * @param userId 用户ID
     * @return 历史面试记录DTO列表
     */
    List<InterviewHistoryDTO> listHistoryWithExperience(@Param("userId") Integer userId);
}
