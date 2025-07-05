package com.echo.virtual_interview.model.dto.interview.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data; /**
 * 半分钟分析报告
 * Half-minute analysis report.
 */
@Data
public class HalfMinuteReport {

    /**
     * 该分析片段在视频中的开始时间（秒）
     * Start time of this analysis segment in the video (in seconds).
     */
    @JsonProperty("start_time_seconds")
    private int startTimeSeconds;

    /**
     * 该分析片段在视频中的结束时间（秒）
     * End time of this analysis segment in the video (in seconds).
     */
    @JsonProperty("end_time_seconds")
    private int endTimeSeconds;

    /**
     * 该区间的实际时长（秒）
     * Actual duration of this interval (in seconds).
     * 注意：JSON中的字段名为 'ration_seconds'，这里使用注解映射。
     * Note: The field name in JSON is 'ration_seconds', mapped here using an annotation.
     */
    @JsonProperty("interval_duration_seconds")
    private int intervalDurationSeconds;

    /**
     * 人脸检出率（0到1）
     * Face detection rate (from 0 to 1).
     */
    @JsonProperty("face_detection_rate")
    private double faceDetectionRate;

    /**
     * 平均注意力分数（0到1）
     * Mean attention score (from 0 to 1).
     */
    @JsonProperty("attention_mean")
    private double attentionMean;

    /**
     * 注意力稳定性（标准差）
     * Attention stability (standard deviation).
     */
    @JsonProperty("attention_stability")
    private double attentionStability;

    /**
     * 区间内的总眨眼次数
     * Total number of blinks in the interval.
     */
    @JsonProperty("total_blinks")
    private int totalBlinks;

    /**
     * 头部姿态的均值和波动性分析
     * Analysis of mean and fluctuation of head pose.
     */
    @JsonProperty("head_pose")
    private HeadPose headPose;

    /**
     * 面部表情代理指标的均值分析
     * Analysis of mean values of facial expression proxy indicators.
     */
    @JsonProperty("expression")
    private Expression expression;

    /**
     * 结构化的紧张度分析报告
     * Structured nervousness analysis report.
     */
    @JsonProperty("nervousness_analysis")
    private NervousnessAnalysis nervousnessAnalysis;
}
