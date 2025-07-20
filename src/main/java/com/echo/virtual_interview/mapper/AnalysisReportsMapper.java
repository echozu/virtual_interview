package com.echo.virtual_interview.mapper;

import com.echo.virtual_interview.model.entity.AnalysisReports;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 存储详细的面试后分析报告 Mapper 接口
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
public interface AnalysisReportsMapper extends BaseMapper<AnalysisReports> {
    /**
     * 根据用户ID查询其所有已完成的面试分析报告
     * @param userId 用户ID
     * @return 分析报告列表
     */
    @Select("SELECT ar.* " +
            "FROM analysis_reports ar " +
            "JOIN interview_sessions s ON ar.session_id = s.id " +
            "WHERE s.user_id = #{userId} AND s.status = '已完成' " +
            "ORDER BY ar.generated_at DESC")
    List<AnalysisReports> findReportsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询其所有已完成面试的平均综合分
     * @param userId 用户ID
     * @return 平均分
     */
    @Select("SELECT AVG(s.overall_score) " +
            "FROM interview_sessions s " +
            "WHERE s.user_id = #{userId} AND s.status = '已完成' AND s.overall_score IS NOT NULL")
    Double findAverageOverallScoreByUserId(@Param("userId") Long userId);
}
