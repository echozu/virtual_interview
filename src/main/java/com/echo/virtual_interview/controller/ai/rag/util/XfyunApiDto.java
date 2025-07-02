package com.echo.virtual_interview.controller.ai.rag.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用于封装讯飞知识库 API 响应的 DTO (Data Transfer Objects)
 */
public class XfyunApiDto {

    /**
     * 【新增】通用的分页数据结构
     * 用于封装包含 'rows' 和 'total' 的响应体
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Page<T> {
        private List<T> rows;
        private int total;
    }

    // 通用响应结构
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BaseResponse<T> {
        private Integer code;
        private String sid;
        private String desc;
        private T data;
    }

    /**
     * 【核心修正】知识库列表响应
     * 将 data 字段的类型从 List<RepoInfo> 修改为 Page<RepoInfo>
     */
    public static class RepoListResponse extends BaseResponse<Page<RepoInfo>> {}

    // 知识库文件列表响应 (这个结构是正确的，保持不变)
    public static class RepoFileListResponse extends BaseResponse<Page<RepoFileInfo>> {}

    // 向量检索响应 (保持不变)
    public static class VectorSearchResponse extends BaseResponse<List<SearchResult>> {}

    // ---------- DTO 内部具体数据结构 (保持不变) ----------

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepoInfo {
        private String repoId;
        private String repoName;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepoFileInfo {
        private String fileId;
        private String fileName;
        private String fileStatus;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResult {
        private String content;
        private Double score;
        private String fileId;
        @JsonProperty("index")
        private Integer chunkIndex;
        private String type;
    }
}