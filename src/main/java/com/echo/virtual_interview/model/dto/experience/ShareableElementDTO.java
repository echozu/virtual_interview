package com.echo.virtual_interview.model.dto.experience;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 可分享的AI报告元素 DTO
 * 用于描述报告中可以被用户选择性公开的模块
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareableElementDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模块的唯一标识 (用于后端JSON存储和前端识别)
     * 例如: "radar_data"
     */
    private String key;

    /**
     * 显示给用户的模块名称
     * 例如: "能力雷达图"
     */
    private String label;

    /**
     * 该选项在前端的默认状态 (true为默认勾选, false为默认不勾选)
     */
    private boolean defaultState;
}