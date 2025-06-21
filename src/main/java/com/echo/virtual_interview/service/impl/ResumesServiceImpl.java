package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.model.entity.Resumes;
import com.echo.virtual_interview.mapper.ResumesMapper;
import com.echo.virtual_interview.service.IResumesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 简历主表，整合了大部分信息 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Service
public class ResumesServiceImpl extends ServiceImpl<ResumesMapper, Resumes> implements IResumesService {

}
