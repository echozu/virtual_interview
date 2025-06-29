package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.model.entity.Chats;
import com.echo.virtual_interview.mapper.ChatsMapper;
import com.echo.virtual_interview.service.IChatsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 聊天会话表 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-29
 */
@Service
public class ChatsServiceImpl extends ServiceImpl<ChatsMapper, Chats> implements IChatsService {

}
