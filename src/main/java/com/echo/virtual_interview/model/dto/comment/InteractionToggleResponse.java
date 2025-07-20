package com.echo.virtual_interview.model.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; /**
 * 点赞/收藏切换操作的响应体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteractionToggleResponse {

    /**
     * 操作后，当前用户是否处于执行状态（例如，是否已点赞）
     */
    private boolean isActive;

    /**
     * 操作后，帖子的最新总数（例如，最新的点赞总数）
     */
    private long totalCount;
}
