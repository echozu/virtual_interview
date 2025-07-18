package com.echo.virtual_interview.model.dto.analysis;

import lombok.Data;

// 对话记录
@Data
public class DialogueDTO {
    private Long id;
    private Integer sequence;
    private String ai_message;
    private String user_message;
    private String timestamp;
    private String turn_analysis;
}
