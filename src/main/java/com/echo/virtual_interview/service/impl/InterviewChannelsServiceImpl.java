package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.model.entity.InterviewChannels;
import com.echo.virtual_interview.mapper.InterviewChannelsMapper;
import com.echo.virtual_interview.service.IInterviewChannelsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 存储面试的配置模板，即“频道” 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Service
public class InterviewChannelsServiceImpl extends ServiceImpl<InterviewChannelsMapper, InterviewChannels> implements IInterviewChannelsService {

}
