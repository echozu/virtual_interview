package com.echo.virtual_interview.model.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

// 雷达图
@Data
@NoArgsConstructor // 添加无参构造函数 (良好的实践，尤其是在反序列化时)
@AllArgsConstructor // 添加包含所有参数的构造函数，这将生成 public Indicator(String name, int max)
public class RadarDataDTO {
    private List<Indicator> indicator;
    private List<SeriesData> series;

    @Data
    @NoArgsConstructor // 添加无参构造函数 (良好的实践，尤其是在反序列化时)
    @AllArgsConstructor
    public static class Indicator {
        private String name;
        private int max=100; // 默认是不能多于100
        /**
         * 为了能方便地使用 new Indicator("专业知识")，
         * 我们需要一个只包含 name 的构造函数。
         * 由于已经有了 @AllArgsConstructor，我们可以手动添加一个更方便的构造函数。
         * @param name 指标名称
         */
        public Indicator(String name) {
            this.name = name;

        }
    }

    @Data
    public static class SeriesData {
        private String name = "能力评分";
        private List<BigDecimal> value;
    }
}
