package com.echo.virtual_interview.mapper;

import com.echo.virtual_interview.model.dto.experience.ExperiencePostQueryRequest;
import com.echo.virtual_interview.model.dto.experience.ExperiencePostVO;
import com.echo.virtual_interview.model.entity.ExperiencePosts;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
/**
 * <p>
 * 面经分享主表 Mapper 接口
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
public interface ExperiencePostsMapper extends BaseMapper<ExperiencePosts> {
    /**
     * 分页查询面经列表
     * @param page 分页对象
     * @param queryParam 查询参数，例如：tagId, company, position等
     * @return
     */
    Page<ExperiencePostVO> listExperiencePostVO(Page<ExperiencePostVO> page, @Param("query") ExperiencePostQueryRequest queryParam);

}
