package com.echo.virtual_interview.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelFilterDTO;
import com.echo.virtual_interview.model.entity.InterviewChannels;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 存储面试的配置模板，即“频道” Mapper 接口
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
public interface InterviewChannelsMapper extends BaseMapper<InterviewChannels> {


    /**
     * 多条件分页查询频道列表
     * @param page 分页对象
     * @param filterDTO 筛选条件 DTO
     * @param topicList 处理过的知识点列表
     * @param topicCount 【新增】知识点列表的大小
     * @return
     */
    Page<InterviewChannels> selectChannelsByPage(Page<InterviewChannels> page,
                                                @Param("filter") ChannelFilterDTO filterDTO,
                                                @Param("topicList") List<String> topicList,
                                                @Param("topicCount") int topicCount,
                                                @Param("currentUserId") Long currentUserId); //【修改】增加 currentUserId 参数



    /**
     * 【新增】根据频道ID查询其所有知识点的名称
     * @param channelId 频道ID
     * @return 知识点名称列表
     */
    List<String> selectTopicNamesByChannelId(@Param("channelId") Long channelId);
    /**
     * 根据用户ID，查询该用户所有已完成面试中使用过的所有频道信息。
     * 使用 DISTINCT 确保每个频道只返回一次。
     * @param userId 用户ID
     * @return 频道列表
     */
    @Select("SELECT DISTINCT ic.* " +
            "FROM interview_channels ic " +
            "JOIN interview_sessions s ON ic.id = s.channel_id " +
            "WHERE s.user_id = #{userId} AND s.status = '已完成' AND s.channel_id IS NOT NULL")
    List<InterviewChannels> findDistinctChannelsByUserId(@Param("userId") Long userId);
}
