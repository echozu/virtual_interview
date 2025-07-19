package com.echo.virtual_interview.adapater;

import com.echo.virtual_interview.model.dto.interview.channel.ChannelDetailDTO;
import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import com.echo.virtual_interview.model.entity.ResumeModule;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class ResumeAndChannelAdapter {

    // 定义内容截取长度
    private static final int MAX_CONTENT_LENGTH = 250;

    /**
     * 优化后的简历格式化方法
     */
    public String formatResumeToMarkdown(ResumeDataDto resume, List<ResumeModule> modules) {
        StringBuilder sb = new StringBuilder();

        // 移除了原有的简历标题和标签，因为它们对提问帮助有限
        // sb.append("**简历标题：** ").append(resume.getBasicInfo().getTitle()).append("\n");
        // sb.append("**标签：** ").append(resume.getBasicInfo().getTag()).append("\n\n");

        modules.stream()
                .filter(module -> module.getIsAppear() != null && module.getIsAppear())
                .forEach(module -> {
                    // 使用更紧凑的标题
                    sb.append("### ").append(module.getTitle()).append(" (").append(module.getModuleType()).append(")\n");

                    if (StringUtils.hasText(module.getSubject())) sb.append("- 主体: ").append(module.getSubject()).append("\n");
                    if (StringUtils.hasText(module.getMajor())) sb.append("- 角色: ").append(module.getMajor()).append("\n");

                    // 使用更紧凑的时间格式
                    if (module.getFromDate() != null) {
                        String toDateStr = (module.getToNow() != null && module.getToNow()) ? "至今" : String.valueOf(module.getToDate());
                        sb.append("- 时间: ").append(module.getFromDate()).append(" ~ ").append(toDateStr).append("\n");
                    }

                    // 对核心内容进行截断
                    if (StringUtils.hasText(module.getContent())) {
                        String content = module.getContent().trim();
                        String truncatedContent = (content.length() > MAX_CONTENT_LENGTH)
                                ? content.substring(0, MAX_CONTENT_LENGTH) + "..."
                                : content;
                        sb.append("- 描述要点:\n").append(truncatedContent).append("\n");
                    }
                    sb.append("\n");
                });
        return sb.toString();
    }

    /**
     * 优化后的频道信息格式化方法
     */
    public String formatChannelToMarkdown(ChannelDetailDTO channel) {
        StringBuilder sb = new StringBuilder();

        // 保留最核心的信息
        sb.append("职位: ").append(channel.getTargetPosition());
        if(StringUtils.hasText(channel.getMajor())) {
            sb.append(" / ").append(channel.getMajor());
        }
        sb.append("\n");
        sb.append("公司: ").append(channel.getTargetCompany()).append("\n");
        // 风格已在系统提示词中注入，此处省略
        // sb.append("**风格：** ").append(channel.getInterviewerStyle()).append("\n");

        if (channel.getTopics() != null && !channel.getTopics().isEmpty()) {
            // 将知识点合并为一行，更紧凑
            String topics = channel.getTopics().stream().collect(Collectors.joining(", "));
            sb.append("重点考察: ").append(topics).append("\n");
        }
        return sb.toString();
    }
}