package com.echo.virtual_interview.model.dto.interview;

import lombok.Data;

import java.util.List;

@Data
public class ChannelFilterDTO {
    // 页数
    private long pageNum = 1;
    // 页数大小
    private long pageSize = 10;

    //工作类型('校招','社招','实习','兼职','不限')
    private String jobType;
    //面试官风格('温和','严肃','技术型','压力面','行为面','随机','自定义')
    private String interviewerStyle;
    //面试形式('一对一', '群面', '多对一')
    private String interviewMode;
    //预计面试时长（分钟）
    private Integer estimatedDuration;
    //专业方向
    private String major;
    //应聘岗位(如后端）
    private String targetPosition;
    //应聘公司
    private String targetCompany;
    
    // topics 可以支持多选，或者模糊匹配一个
    private String topic; 

    // usageCount的排序条件: "正序" 或 "倒序"
    private String usageCountSort ="正序";
}