package com.echo.virtual_interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echo.virtual_interview.model.entity.Topic;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TopicMapper extends BaseMapper<Topic> {
}