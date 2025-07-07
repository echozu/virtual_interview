package com.echo.virtual_interview.mapper;

import com.echo.virtual_interview.model.entity.InterviewDialogue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 存储面试过程中的每一轮对话及初步分析 Mapper 接口
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
public interface InterviewDialogueMapper extends BaseMapper<InterviewDialogue> {

    @Select("""
        SELECT * FROM interview_dialogue
        WHERE session_id = #{sessionId}
        ORDER BY sequence DESC
        LIMIT #{limit}
    """)
    List<InterviewDialogue> selectLastNMessages(@Param("sessionId") String sessionId, @Param("limit") int limit);

    @Select("""
        SELECT COALESCE(MAX(sequence), 0) FROM interview_dialogue WHERE session_id = #{sessionId}
    """)
    int getMaxSequence(@Param("sessionId") String sessionId);

    @Delete("""
        DELETE FROM interview_dialogue WHERE session_id = #{sessionId}
    """)
    void deleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 查找指定会话中最新的一条等待用户回答的对话记录
     * (即 user_message 为 NULL 的记录)
     * @param sessionId 会话ID
     * @return InterviewDialogue 实体
     */
    @Select("SELECT * FROM interview_dialogue WHERE session_id = #{sessionId} AND user_message IS NULL ORDER BY sequence DESC LIMIT 1")
    InterviewDialogue findLatestPendingDialogue(@Param("sessionId") String sessionId);
}
