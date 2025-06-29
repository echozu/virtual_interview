package com.echo.virtual_interview.mapper;

import com.echo.virtual_interview.model.entity.ChatsMessages;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 存储每个会话中的具体聊天记录 Mapper 接口
 * </p>
 *
 * @author echo
 * @since 2025-06-29
 */
public interface ChatsMessagesMapper extends BaseMapper<ChatsMessages> {
    /**
     * 根据 chatId 查询最近的 N 条消息，并按时间升序返回
     * @param chatId 会话ID
     * @param lastN 要查询的消息数量
     * @return 消息列表
     */
    List<ChatsMessages> selectLastNMessages(@Param("chatId") String chatId, @Param("lastN") int lastN);
}
