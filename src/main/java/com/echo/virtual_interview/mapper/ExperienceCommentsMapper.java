package com.echo.virtual_interview.mapper;

import com.echo.virtual_interview.model.entity.ExperienceComments;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 面经评论表 Mapper 接口
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
public interface ExperienceCommentsMapper extends BaseMapper<ExperienceComments> {
    /**
     * 计算用户所有面经收到的总评论数
     * @param userId 用户ID
     * @return 总评论数
     */
    @Select("SELECT COUNT(*) FROM experience_comments c " +
            "JOIN experience_posts p ON c.post_id = p.id " +
            "WHERE p.user_id = #{userId}")
    Long countTotalCommentsByUserId(@Param("userId") Long userId);
}
