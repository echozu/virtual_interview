package com.echo.virtual_interview.service;

import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import com.echo.virtual_interview.model.entity.ResumeModule;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 简历模块表 服务类
 * </p>
 *
 * @author echo
 * @since 2025-06-22
 */
public interface IResumeModuleService extends IService<ResumeModule> {
    /**
     * 根据简历id获取model
     */
    List<ResumeModule> getResumeModulesByResumId(Long resumeId);
}
