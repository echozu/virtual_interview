# -*- coding: utf-8 -*-

import numpy as np

# --- 可配置的分析阈值 ---
# 注意力分数的阈值，低于此值被认为是“不专注”
ATTENTION_THRESHOLD = 0.4
# 连续多少秒不专注，才被记录为一次“分心事件”
DISTRACTION_DURATION_SECONDS = 3


def analyze_interview_data(analysis_by_second):
    """
    对处理过的逐秒分析数据进行深度分析，生成面试报告。
    :param analysis_by_second: 一个列表，包含逐秒的分析结果，
                               例如 [{'time_in_seconds': 1, 'attention_mean': 0.8, 'blink_count': 0}, ...]
    :return: 一个包含分析报告的字典。
    """
    if not analysis_by_second:
        return {
            "distraction_events": [],
            "attention_stability": 0,
            "average_blink_rate": 0,
            "summary": "无有效数据进行分析。"
        }

    # 注意：此模块仍基于秒级数据，您可以选择保留它或根据半分钟数据创建新的摘要逻辑
    attention_scores = [item.get('attention_mean', 0) for item in analysis_by_second]
    blink_counts = [item.get('blink_count', 0) for item in analysis_by_second]

    # 1. 分析分心事件
    distraction_events = []
    distraction_start_time = None
    distraction_counter = 0

    for i, item in enumerate(analysis_by_second):
        time_sec = item.get('time_in_seconds', i + 1)
        if item.get('attention_mean', 1.0) < ATTENTION_THRESHOLD:
            if distraction_start_time is None:
                distraction_start_time = time_sec
            distraction_counter += 1
        else:
            if distraction_counter >= DISTRACTION_DURATION_SECONDS:
                distraction_events.append({
                    "start_time_seconds": distraction_start_time,
                    "end_time_seconds": time_sec - 1,
                    "duration_seconds": distraction_counter
                })
            distraction_start_time = None
            distraction_counter = 0

    if distraction_counter >= DISTRACTION_DURATION_SECONDS and distraction_start_time is not None:
        distraction_events.append({
            "start_time_seconds": distraction_start_time,
            "end_time_seconds": analysis_by_second[-1].get('time_in_seconds', len(analysis_by_second)),
            "duration_seconds": distraction_counter
        })

    # 2. 计算注意力稳定性 (使用标准差)
    attention_stability = np.std(attention_scores) if attention_scores else 0

    # 3. 计算平均眨眼频率 (每秒眨眼次数)
    total_blinks = sum(blink_counts)
    total_seconds = len(blink_counts)
    average_blink_rate = total_blinks / total_seconds if total_seconds > 0 else 0

    # 4. 生成总结性报告
    summary = f"分析完毕。共检测到 {len(distraction_events)} 次明显分心事件。"
    summary += f" 注意力稳定性得分为 {attention_stability:.3f} (分数越低越稳定)。"
    summary += f" 平均每秒眨眼频率为 {average_blink_rate:.2f} 次。"

    if attention_stability > 0.4:
        summary += " 候选人注意力波动较大，可能表现出紧张或焦虑。"
    elif len(distraction_events) > 2:
        summary += " 候选人分心次数较多，建议关注其投入度。"
    else:
        summary += " 候选人整体表现较为专注稳定。"

    return {
        "distraction_events": distraction_events,
        "attention_stability": round(attention_stability, 3),
        "average_blink_rate": round(average_blink_rate, 2),
        "summary": summary
    }

