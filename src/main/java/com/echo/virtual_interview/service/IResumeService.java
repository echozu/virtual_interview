package com.echo.virtual_interview.service;

import com.echo.virtual_interview.model.dto.interview.andriod.ResumeData;
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
    /**
     * 安卓端-保存或更新简历
     * @param resumeData 安卓端简历的完整数据
     * @param userId 当前登录用户ID
     */
    void androidSaveOrUpdateResume(ResumeData resumeData, Integer userId);

    /**
     * 安卓端-根据当前登录用户信息获取简历
     * @param userId 当前登录用户ID
     * @return 组装好的安卓端简历数据
     */
    ResumeData androidGetResumeByUserId(Integer userId);
   }
