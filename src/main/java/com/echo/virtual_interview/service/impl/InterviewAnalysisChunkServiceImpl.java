package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.model.entity.InterviewAnalysisChunk;
import com.echo.virtual_interview.mapper.InterviewAnalysisChunkMapper;
import com.echo.virtual_interview.service.IInterviewAnalysisChunkService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 存储面试过程中每30秒一次的详细行为分析数据块(V2-简化版) 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-07-04
 */
@Service
public class InterviewAnalysisChunkServiceImpl extends ServiceImpl<InterviewAnalysisChunkMapper, InterviewAnalysisChunk> implements IInterviewAnalysisChunkService {

}
