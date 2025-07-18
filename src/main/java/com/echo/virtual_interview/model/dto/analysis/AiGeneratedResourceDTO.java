package com.echo.virtual_interview.model.dto.analysis;

import lombok.Data;

import java.util.List;

// DTO for parsing AI-generated resources
    @Data
    public class AiGeneratedResourceDTO {
    private TopicDetail topic;
    private List<ResourceDetail> resources;

    @Data
    public static class TopicDetail {
        private String name;
        private String description;
        private String category;
    }

    @Data
    public static class ResourceDetail {
        private String title;
        private String description;
        private String url;
        private String resource_type; // 使用String以增加兼容性
        private String difficulty; // 使用String以增加兼容性
    }
}
