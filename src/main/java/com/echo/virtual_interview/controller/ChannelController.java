package com.echo.virtual_interview.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelCardDTO;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelCreateDTO;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelDetailDTO;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelFilterDTO;
import com.echo.virtual_interview.model.entity.InterviewChannels;
import com.echo.virtual_interview.service.IInterviewChannelsService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 面试频道接口
 */
@RestController
@RequestMapping("/api/interview/channel")
@Slf4j
public class ChannelController {

    @Resource
    private final IInterviewChannelsService interviewChannelService;

    public ChannelController(IInterviewChannelsService interviewChannelService) {
        this.interviewChannelService = interviewChannelService;
    }

    /**
     * 获取筛选分类信息
     */
    @GetMapping("/filters")
    public BaseResponse<Map<String, Object>> getFilterOptions() {
        return ResultUtils.success(interviewChannelService.getFilterOptions());
    }

    /**
     * 分页筛选面试频道卡片
     * GET /api/interview/channel?pageNum=1&pageSize=10&jobType=校招&usageCountSort=desc
     */
    @GetMapping("")
    public BaseResponse<Page<ChannelCardDTO>> listChannels(ChannelFilterDTO filterDTO) {
        Page<ChannelCardDTO> page = interviewChannelService.listChannelsByPage(filterDTO);
        return ResultUtils.success(page);
    }

    /**
     * 获取指定ID的频道详细信息
     * GET /api/interview/channels/{id}
     */
    @GetMapping("/{id}")
    public BaseResponse<ChannelDetailDTO> getChannelDetails(@PathVariable Long id) {
        ChannelDetailDTO channel = interviewChannelService.getChannelDetails(id);
        if (channel != null) {
            return ResultUtils.success(channel);
        }
        return ResultUtils.error(ErrorCode.PARAMS_ERROR);
    }

    /**
     * 用户创建自定义频道
     * POST /api/interview/channel
     */
    @PostMapping
    public ResponseEntity<InterviewChannels> createChannel(@Valid @RequestBody ChannelCreateDTO createDTO) {
        Integer userIdContext = UserIdContext.getUserIdContext();
        InterviewChannels createdChannel = interviewChannelService.createChannel(createDTO, Long.valueOf(userIdContext));
        return ResponseEntity.ok(createdChannel);
    }
}
