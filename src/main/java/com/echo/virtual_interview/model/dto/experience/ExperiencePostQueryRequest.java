package com.echo.virtual_interview.model.dto.experience;

import lombok.Data;
import java.util.List;

/**
 * 面经帖子分页查询请求体
 */
@Data
public class ExperiencePostQueryRequest {

    /**
     * 当前页码
     */
    private long current = 1;

    /**
     * 每页数量
     */
    private long pageSize = 10;

    /**
     * 搜索关键词 (用于模糊查询标题和内容)
     */
    private String searchText;

    /**
     * 排序字段。
     * 可选值: "hot" (综合热度-默认), "latest" (最新发布), "mostCollected" (最多收藏)
     */
    private String sortField;
    
    /**
     * 需要筛选的标签列表 (AND关系，即需要同时包含所有标签)
     */
    private List<String> tags;

    // --- 新增筛选字段 ---

    /**
     * 公司名称 (模糊匹配)
     */
    private String company;

    /**
     * 工作类型 (精确匹配, 如 '校招', '社招')
     */
    private String jobType;

    /**
     * 应聘岗位 (模糊匹配, 如 '后端', '产品')
     */
    private String position;
}