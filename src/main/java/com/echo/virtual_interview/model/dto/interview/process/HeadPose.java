package com.echo.virtual_interview.model.dto.interview.process;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data; /**
 * 头部姿态分析
 * Head pose analysis.
 */
@Data
public class HeadPose {

    @JsonProperty("pitch_mean")
    private double pitchMean;

    @JsonProperty("yaw_mean")
    private double yawMean;

    @JsonProperty("roll_mean")
    private double rollMean;

    @JsonProperty("pitch_fluctuation")
    private double pitchFluctuation;

    @JsonProperty("yaw_fluctuation")
    private double yawFluctuation;

    @JsonProperty("roll_fluctuation")
    private double rollFluctuation;
}
