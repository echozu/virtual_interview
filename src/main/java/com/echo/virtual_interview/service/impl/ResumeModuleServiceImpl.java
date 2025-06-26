package com.echo.virtual_interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.echo.virtual_interview.model.entity.ResumeModule;
import com.echo.virtual_interview.mapper.ResumeModuleMapper;
import com.echo.virtual_interview.service.IResumeModuleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 简历模块表 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-22
 */
@Service
public class ResumeModuleServiceImpl extends ServiceImpl<ResumeModuleMapper, ResumeModule> implements IResumeModuleService {

    @Resource
    private ResumeModuleMapper resumeModuleMapper;
    @Override
    public List<ResumeModule> getResumeModulesByResumId(Long resumeId) {
        return resumeModuleMapper.selectList(new QueryWrapper<>(new ResumeModule().setResumeId(resumeId)));
    }
}
