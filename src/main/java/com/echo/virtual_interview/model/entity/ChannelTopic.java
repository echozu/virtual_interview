package com.echo.virtual_interview.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@TableName("channel_topics")
@AllArgsConstructor // 添加一个方便的全参构造函数
public class ChannelTopic {
    private Long channelId;
    private Integer topicId;
}