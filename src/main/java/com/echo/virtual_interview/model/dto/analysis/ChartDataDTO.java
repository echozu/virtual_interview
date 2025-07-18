package com.echo.virtual_interview.model.dto.analysis;

import lombok.Data;

import java.util.List;

// 通用图表数据 (用于紧张度、口头禅、眼神)
@Data
public class ChartDataDTO {
    private List<String> xAxisData;
    private List<Object> seriesData;
}
