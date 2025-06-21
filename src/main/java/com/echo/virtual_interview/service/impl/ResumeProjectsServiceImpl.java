package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.model.entity.ResumeProjects;
import com.echo.virtual_interview.mapper.ResumeProjectsMapper;
import com.echo.virtual_interview.service.IResumeProjectsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 简历的项目经历（可多个） 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Service
public class ResumeProjectsServiceImpl extends ServiceImpl<ResumeProjectsMapper, ResumeProjects> implements IResumeProjectsService {

}
