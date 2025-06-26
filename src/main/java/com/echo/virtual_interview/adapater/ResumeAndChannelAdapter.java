package com.echo.virtual_interview.adapater;

import com.echo.virtual_interview.model.dto.interview.ChannelDetailDTO;
import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import com.echo.virtual_interview.model.entity.ResumeModule;

import java.util.List;

public class ResumeAndChannelAdapter {
    public String formatResumeToMarkdown(ResumeDataDto resume, List<ResumeModule> modules) {
        StringBuilder sb = new StringBuilder();

        sb.append("**简历标题：** ").append(resume.getBasicInfo().getTitle()).append("\n");
        sb.append("**标签：** ").append(resume.getBasicInfo().getTag()).append("\n\n");

        modules.stream()
                .filter(ResumeModule::getIsAppear)
                .forEach(module -> {
                    sb.append("### ").append(module.getModuleType()).append("：").append(module.getTitle()).append("\n");
                    if (module.getSubject() != null) sb.append("- **主体**：").append(module.getSubject()).append("\n");
                    if (module.getMajor() != null) sb.append("- **角色/方向**：").append(module.getMajor()).append("\n");
                    if (module.getFromDate() != null) sb.append("- **起止时间：** ").append(module.getFromDate())
                            .append(" ~ ").append(module.getToNow() != null && module.getToNow()? "至今" : module.getToDate()).append("\n");
                    if (module.getContent() != null) sb.append("- **描述**：\n").append(module.getContent()).append("\n");
                    sb.append("\n");
                });
        return sb.toString();
    }
    public String formatChannelToMarkdown(ChannelDetailDTO channel) {
        StringBuilder sb = new StringBuilder();

        sb.append("**频道标题：** ").append(channel.getTitle()).append("\n");
        sb.append("**岗位方向：** ").append(channel.getTargetPosition()).append(" / ").append(channel.getMajor()).append("\n");
        sb.append("**面试公司：** ").append(channel.getTargetCompany()).append("\n");
        sb.append("**风格：** ").append(channel.getInterviewerStyle()).append("\n");
        sb.append("**面试形式：** ").append(channel.getInterviewMode()).append("\n");
        sb.append("**预计时长：** ").append(channel.getEstimatedDuration()).append("分钟\n");
        sb.append("**频道描述：** ").append(channel.getDescription()).append("\n");

        if (channel.getTopics() != null && !channel.getTopics().isEmpty()) {
            sb.append("**考察知识点：**\n");
            for (String topic : channel.getTopics()) {
                sb.append("- ").append(topic).append("\n");
            }
        }
        return sb.toString();
    }
}
