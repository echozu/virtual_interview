package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.model.entity.InterviewDialogue;
import com.echo.virtual_interview.mapper.InterviewDialogueMapper;
import com.echo.virtual_interview.service.IInterviewDialogueService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 存储面试过程中的每一轮对话及初步分析 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Service
public class InterviewDialogueServiceImpl extends ServiceImpl<InterviewDialogueMapper, InterviewDialogue> implements IInterviewDialogueService {

}
