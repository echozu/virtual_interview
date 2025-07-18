package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.model.entity.LearningResources;
import com.echo.virtual_interview.mapper.LearningResourcesMapper;
import com.echo.virtual_interview.service.ILearningResourcesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用于个性化推荐的学习资源库 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-07-18
 */
@Service
public class LearningResourcesServiceImpl extends ServiceImpl<LearningResourcesMapper, LearningResources> implements ILearningResourcesService {

}
