package com.echo.virtual_interview.service;

import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import com.echo.virtual_interview.model.entity.Resume;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 简历主表 服务类
 * </p>
 *
 * @author echo
 * @since 2025-06-22
 */
public interface IResumeService extends IService<Resume> {
    /**
     * 创建或更新简历
     * @param resumeDataDto 前端传入的简历数据
     * @param userId 用户ID
     */
    void saveOrUpdateResume(ResumeDataDto resumeDataDto, Integer userId);

    /**
     * 根据用户ID获取简历
     * @param userId 用户ID
     * @return 组装好的简历数据
     */
    ResumeDataDto getResumeByUserId(Integer userId);
}
