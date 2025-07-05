package com.echo.virtual_interview.model.dto.interview.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 顶层载荷对象，用于接收Python服务发送的完整分析结果。
 * Top-level payload object to receive the complete analysis result from the Python service.
 */
@Data
public class VideoAnalysisPayload {

    /**
     * 本次分析任务的唯一ID
     * Unique ID for this analysis task.
     */
    private String analysisId;

    /**
     * 所属的面试会话ID
     * The session ID of the interview.
     */
    private String sessionId;

    /**
     * 请求状态, "success" 表示成功
     * Request status, "success" indicates success.
     */
    private String status;

    /**
     * 描述信息
     * Descriptive message.
     */
    private String message;

    /**
     * 核心数据，包含按半分钟聚合的详细分析报告
     * Core data, containing detailed analysis reports aggregated by half-minute intervals.
     */
    @JsonProperty("analysis_by_half_minute")
    private List<HalfMinuteReport> analysisByHalfMinute;

    /**
     * 基于秒级数据的整体文字摘要
     * An overall text summary based on second-by-second data.
     */
    @JsonProperty("overall_summary")
    private Map<String, Object> overallSummary;

    /**
     * 包含逐秒的原始数据
     * Contains raw data on a second-by-second basis.
     */
    @JsonProperty("raw_data_by_second")
    private List<Map<String, Object>> rawDataBySecond;

    /**
     * 视频第一帧的Base64编码字符串
     * Base64 encoded string of the first frame of the video.
     */
    @JsonProperty("first_frame_base64")
    private String firstFrameBase64;

    /**
     * 视频最后一帧的Base64编码字符串
     * Base64 encoded string of the last frame of the video.
     */
    @JsonProperty("last_frame_base64")
    private String lastFrameBase64;
}

/**
 * 表情分析
 * Expression analysis.
 */
@Data
class Expression {

    @JsonProperty("mouth_opening_mean")
    private double mouthOpeningMean;

    @JsonProperty("mouth_smile_mean")
    private double mouthSmileMean;
}

/**
 * 紧张度分析的子组件
 * Sub-component of nervousness analysis.
 */
@Data
class NervousnessComponent {

    /**
     * 该指标在本时间段内的实际测量值
     * The actual measured value of this indicator in this time period.
     */
    private double value;

    /**
     * 值的单位，如 "次/半分钟"
     * The unit of the value, e.g., "times/half-minute".
     */
    private String unit;

    /**
     * 该指标的正常范围参考值
     * The normal range reference value for this indicator.
     */
    @JsonProperty("normal_range")
    private List<Integer> normalRange;

    /**
     * 用于判断该指标是否异常的阈值（高）
     * The threshold (high) used to determine if this indicator is abnormal.
     */
    @JsonProperty("threshold_high")
    private Double thresholdHigh;

    /**
     * 用于判断该指标是否异常的阈值（低）
     * The threshold (low) used to determine if this indicator is abnormal.
     */
    @JsonProperty("threshold_low")
    private Double thresholdLow;

    /**
     * 该指标对 overall_score 的贡献分数
     * The score contribution of this indicator to the overall_score.
     */
    @JsonProperty("score_contribution")
    private int scoreContribution;

    /**
     * 对该指标表现的简短文字解释
     * A brief textual explanation of the performance of this indicator.
     */
    private String comment;
}
