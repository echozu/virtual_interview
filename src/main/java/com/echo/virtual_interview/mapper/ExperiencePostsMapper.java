package com.echo.virtual_interview.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
     * 自定义分页查询，关联多表获取面经VO列表
     * @param page 分页对象
     * @param query 查询条件
     * @return 分页的VO结果
     */
    IPage<ExperiencePostVO> selectPostVOPage(Page<?> page, @Param("query") ExperiencePostQueryRequest query);
}
