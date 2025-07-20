package com.echo.virtual_interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echo.virtual_interview.model.dto.career.CareerAssessment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CareerAssessmentMapper extends BaseMapper<CareerAssessment> {
    // BaseMapper已提供常用CRUD，暂时无需自定义方法
}