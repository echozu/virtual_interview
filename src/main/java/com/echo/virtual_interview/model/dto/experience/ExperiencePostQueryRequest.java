package com.echo.virtual_interview.model.dto.experience;

import lombok.Data;
import java.io.Serializable;

/**
 * 查询面经列表的请求体 (Query DTO)
 * 封装了分页、排序、筛选等所有查询参数
 */
@Data
public class ExperiencePostQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private long current = 1;

    /**
     * 每页显示条数
     */
    private long pageSize = 10;


    /**
     * 排序方式
     * 可选值: "latest" (最新发布), "mostCollected" (最多收藏), "hot" (综合热度)
     */
    private String sortBy = "hot"; // Default sorting by hotness

    // --- Filtering Parameters ---

    /**
     * 全文搜索关键字 (将匹配标题)
     */
    private String searchKey;

    /**
     * 根据标签ID进行筛选
     */
    private Long tagId;

    /**
     * 根据公司名称进行筛选
     */
    private String company;

    /**
     * 根据应聘岗位进行筛选
     */
    private String position;
    
    /**
     * 根据工作类型进行筛选 (e.g., "校招", "社招")
     */
    private String jobType;

    /**
     * 根据专业方向进行筛选
     */
    private String major;

}