package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.model.entity.ChatsMessages;
import com.echo.virtual_interview.mapper.ChatsMessagesMapper;
import com.echo.virtual_interview.service.IChatsMessagesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 存储每个会话中的具体聊天记录 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-29
 */
@Service
public class ChatsMessagesServiceImpl extends ServiceImpl<ChatsMessagesMapper, ChatsMessages> implements IChatsMessagesService {

}
