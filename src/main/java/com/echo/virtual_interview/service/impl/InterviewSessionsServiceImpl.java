package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.model.entity.InterviewSessions;
import com.echo.virtual_interview.mapper.InterviewSessionsMapper;
import com.echo.virtual_interview.service.IInterviewSessionsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 记录每一次具体的面试实例 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Service
public class InterviewSessionsServiceImpl extends ServiceImpl<InterviewSessionsMapper, InterviewSessions> implements IInterviewSessionsService {

}
