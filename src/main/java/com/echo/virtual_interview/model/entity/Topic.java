package com.echo.virtual_interview.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("topics")
@NoArgsConstructor // 添加无参构造函数
public class Topic {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;

    public Topic(String name) { // 添加一个方便的构造函数
        this.name = name;
    }
}