package com.echo.virtual_interview.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echo.virtual_interview.model.dto.interview.ChannelCardDTO;
import com.echo.virtual_interview.model.dto.interview.ChannelCreateDTO;
import com.echo.virtual_interview.model.dto.interview.ChannelDetailDTO;
import com.echo.virtual_interview.model.dto.interview.ChannelFilterDTO;
import com.echo.virtual_interview.model.entity.InterviewChannels;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 存储面试的配置模板，即“频道” 服务类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
public interface IInterviewChannelsService extends IService<InterviewChannels> {
    /**
     * 功能 1: 获取所有筛选分类信息
     */
    Map<String, Object> getFilterOptions();

    /**
     * 功能 2 & 4: 分页并筛选面试频道
     */
    Page<ChannelCardDTO> listChannelsByPage(ChannelFilterDTO filterDTO);

    /**
     * 获取频道详细信息
     */
    ChannelDetailDTO getChannelDetails(Long id);
    /**
     * 获取频道详细信息，不用加一版本
     */
    ChannelDetailDTO getChannelDetailsNoAdd(Long id);
    /**
     * 功能 3: 创建自定义面试频道
     */
    InterviewChannels createChannel(ChannelCreateDTO createDTO, Long creatorId);
}
