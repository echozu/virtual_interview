package com.echo.virtual_interview.model.dto.interview.audio;

import lombok.Data;

@Data
public class AudioChunkDto {
    private String audio; // 用于接收Base64编码的音频字符串
}