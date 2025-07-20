package com.echo.virtual_interview.mapper;


import com.echo.virtual_interview.model.dto.experience.HotPostDTO;
import com.echo.virtual_interview.model.dto.experience.TopAuthorDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface LeaderboardMapper {

    /**
     * 查询本周点赞数最多的面经帖子
     * @param startTime 本周的开始时间
     * @param limit 查询数量
     * @return 热门帖子列表
     */

    List<Map<String, Object>> findTopLikedPostsInPeriod(
                                                         @Param("startTime") LocalDateTime startTime,
                                                         @Param("limit") int limit
    );

    /**
     * 查询本月获赞最多的用户
     * @param startTime 本月的开始时间
     * @param limit 查询数量
     * @return 热门作者列表
     */
    List<TopAuthorDTO> findTopLikedAuthorsInPeriod(
            @Param("startTime") LocalDateTime startTime,
            @Param("limit") int limit
    );
}