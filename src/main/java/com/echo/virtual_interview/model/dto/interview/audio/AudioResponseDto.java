package com.echo.virtual_interview.model.dto.interview.audio;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AudioResponseDto {
    /**
     * Base64编码的音频数据 (MP3格式)
     */
    private String audio;
    
    /**
     * 标记这是否是最后一个音频块
     */
    private boolean isFinal;
}