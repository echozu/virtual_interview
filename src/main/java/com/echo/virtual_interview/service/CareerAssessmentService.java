package com.echo.virtual_interview.service;


import com.echo.virtual_interview.model.dto.career.CareerProfileVo;

public interface CareerAssessmentService {
    
    /**
     * 生成或获取指定用户的职业评测报告。
     * 业务逻辑会先尝试从数据库获取，如果不存在或需要更新，则重新生成。
     * @param userId 用户ID
     * @return 职业评测报告视图对象
     */
    CareerProfileVo generateAndGetCareerProfile(Long userId);
}