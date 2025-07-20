package com.echo.virtual_interview.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.echo.virtual_interview.controller.ai.InterviewExpert;
import com.echo.virtual_interview.mapper.AnalysisReportsMapper;
import com.echo.virtual_interview.mapper.CareerAssessmentMapper;
import com.echo.virtual_interview.mapper.InterviewChannelsMapper;
import com.echo.virtual_interview.mapper.UsersMapper;
import com.echo.virtual_interview.model.dto.career.CareerAssessment;
import com.echo.virtual_interview.model.dto.career.CareerProfileVo;
import com.echo.virtual_interview.model.entity.AnalysisReports;
import com.echo.virtual_interview.model.entity.InterviewChannels;
import com.echo.virtual_interview.model.entity.Users;
import com.echo.virtual_interview.service.CareerAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CareerAssessmentServiceImpl implements CareerAssessmentService {

    private final AnalysisReportsMapper analysisReportMapper;
    private final CareerAssessmentMapper careerAssessmentMapper;
    private final UsersMapper userMapper;
    private final InterviewExpert interviewExpert;
    private final InterviewChannelsMapper interviewChannelsMapper;

    @Override
    @Cacheable(cacheNames = "GetCareerProfile",key = "#userId")
    public CareerProfileVo generateAndGetCareerProfile(Long userId) {
        List<AnalysisReports> reports = analysisReportMapper.findReportsByUserId(userId);

        if (CollectionUtils.isEmpty(reports)) {
            return CareerProfileVo.empty(userId);
        }

        // [新增] 获取用户面试过的所有目标频道信息
        List<InterviewChannels> targetChannels = interviewChannelsMapper.findDistinctChannelsByUserId(userId);

        Map<String, Double> averageScores = calculateAverageScores(reports);
        Double averageOverallScore = analysisReportMapper.findAverageOverallScoreByUserId(userId);

        // [修改] 将频道信息传递给 buildAiMessage
        String message = buildAiMessage(reports, averageScores, targetChannels);
        String finalPrompt = buildAiPrompt().replace("{ANALYSIS_DATA}", message);
        String rawAiResponse = interviewExpert.aiInterviewGeneratePersonalReport(finalPrompt);

        if (!StringUtils.hasText(rawAiResponse)) {
            throw new RuntimeException("AI服务未能返回任何内容，请稍后重试。");
        }

        String extractedJson = JsonExtractor.extractJson(rawAiResponse);

        if (!StringUtils.hasText(extractedJson)) {
            throw new RuntimeException("AI服务返回格式错误，无法解析评测报告。");
        }

        CareerAssessment aiGeneratedAssessment = JSON.parseObject(extractedJson, CareerAssessment.class);

        if (aiGeneratedAssessment == null) {
            throw new RuntimeException("解析AI评测报告失败。");
        }

        CareerAssessment finalAssessment = enrichAndSaveAssessment(aiGeneratedAssessment, userId, reports.size(), averageOverallScore, averageScores);
        return buildCareerProfileVo(finalAssessment);
    }

    // The following methods do not need changes.
    private Map<String, Double> calculateAverageScores(List<AnalysisReports> reports) {
        Map<String, Double> scoreSums = new HashMap<>();
        Map<String, Integer> scoreCounts = new HashMap<>();
        for (AnalysisReports report : reports) {
            addScore(scoreSums, scoreCounts, "professional_knowledge", report.getScoreProfessionalKnowledge());
            addScore(scoreSums, scoreCounts, "skill_match", report.getScoreSkillMatch());
            addScore(scoreSums, scoreCounts, "verbal_expression", report.getScoreVerbalExpression());
            addScore(scoreSums, scoreCounts, "logical_thinking", report.getScoreLogicalThinking());
            addScore(scoreSums, scoreCounts, "innovation", report.getScoreInnovation());
            addScore(scoreSums, scoreCounts, "stress_resistance", report.getScoreStressResistance());
        }
        Map<String, Double> averageScores = new HashMap<>();
        for (String key : scoreSums.keySet()) {
            double avg = scoreSums.get(key) / scoreCounts.get(key);
            averageScores.put(key, BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue());
        }
        return averageScores;
    }

    private void addScore(Map<String, Double> sums, Map<String, Integer> counts, String key, BigDecimal score) {
        if (score != null) {
            sums.put(key, sums.getOrDefault(key, 0.0) + score.doubleValue());
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
    }

    private String buildAiPrompt() {
        return """
            你是一位资深的HR专家和职业规划师，极其擅长深度分析候选人的综合能力和岗位匹配度。
            你的任务是根据我提供的候选人**面试表现数据**和**目标岗位信息**，生成一份结构化的职业评测报告。

            # 核心规则
            1.  **循证分析**：你的每一句结论都必须基于我提供的数据，不允许凭空想象。
            2.  **对比分析**：[重要] 你必须将候选人的能力得分和表现，与他面试过的目标岗位进行对比分析。
            3.  **专业且有建设性**：语言要专业、精炼，并提供可行的建议。
            4.  **严格JSON输出**：必须严格按照我要求的JSON格式输出，最好将JSON包裹在 ` ```json ... ``` ` 中。

            # 输入数据 (你分析的依据)
            {ANALYSIS_DATA}

            # 输出要求 (必须是可直接解析的JSON)
            输出的JSON对象的字段必须严格包含以下键，其值必须为字符串：
            "strengthsSummary"       (综合优势总结)
            "improvementSummary"     (待改进领域总结)
            "behavioralHabitsSummary"(综合行为习惯，如紧张度、填充词等)
            "positionFitAnalysis"    (深入分析候选人的能力与其目标岗位的匹配程度。明确指出哪些能力达标，哪些是短板，并给出针对性的弥补建议。)
            "careerSuggestions"      (综合性的、长远的职业发展建议)
            "suitablePositions"      (基于其实际表现，推荐真正适合他的岗位方向)
            
            请严格按照指定的JSON格式输出，以便系统存储分析结果。
            """;
    }

    private String buildAiMessage(List<AnalysisReports> reports, Map<String, Double> averageScores, List<InterviewChannels> channels) {
        StringBuilder sb = new StringBuilder();

        // [新增] 在最前面加入目标岗位信息，为AI提供对比的靶子
        if (!CollectionUtils.isEmpty(channels)) {
            sb.append("# 候选人历史面试过的目标岗位与公司\n");
            String positions = channels.stream()
                    .map(InterviewChannels::getTargetPosition)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.joining(", "));
            String companies = channels.stream()
                    .map(InterviewChannels::getTargetCompany)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.joining(", "));

            if (StringUtils.hasText(positions)) {
                sb.append("- **目标岗位**: ").append(positions).append("\n");
            }
            if (StringUtils.hasText(companies)) {
                sb.append("- **目标公司**: ").append(companies).append("\n");
            }
            sb.append("\n---\n\n");
        }

        sb.append("# 候选人的综合面试表现数据\n");
        sb.append("用户职业评测数据如下：\n");
        sb.append("分析基于 ").append(reports.size()).append(" 场面试。\n\n");
        sb.append("核心能力平均分（满分100）：\n");
        averageScores.forEach((key, value) -> sb.append("- ").append(translateScoreKey(key)).append(": ").append(value).append("\n"));
        sb.append("\n历次面试表现摘要：\n");
        for (int i = 0; i < reports.size(); i++) {
            AnalysisReports report = reports.get(i);
            sb.append("\n--- 面试 ").append(i + 1).append(" (").append(report.getGeneratedAt()).append(") ---\n");
            sb.append("总体概述: ").append(report.getOverallSummary()).append("\n");
            sb.append("行为分析: ").append(report.getBehavioralAnalysis()).append("\n");
            if (report.getFillerWordUsage() != null) {
                sb.append("填充词使用情况: ").append(report.getFillerWordUsage()).append("\n");
            }
        }
        return sb.toString();
    }

    private String translateScoreKey(String key) {
        switch (key) {
            case "professional_knowledge": return "专业知识";
            case "skill_match": return "技能匹配";
            case "verbal_expression": return "语言表达";
            case "logical_thinking": return "逻辑思维";
            case "innovation": return "创新能力";
            case "stress_resistance": return "应变抗压";
            default: return key;
        }
    }

    /**
     * **[FIXED]** This method now re-fetches the object after saving to get DB-generated values.
     */
    private CareerAssessment enrichAndSaveAssessment(CareerAssessment aiGeneratedAssessment, Long userId, int totalInterviews, Double avgOverallScore, Map<String, Double> avgScores) {
        CareerAssessment finalAssessment = careerAssessmentMapper.selectOne(new QueryWrapper<CareerAssessment>().eq("user_id", userId));

        if (finalAssessment == null) {
            finalAssessment = aiGeneratedAssessment;
            finalAssessment.setUserId(userId);
        } else {
            finalAssessment.setStrengthsSummary(aiGeneratedAssessment.getStrengthsSummary());
            finalAssessment.setImprovementSummary(aiGeneratedAssessment.getImprovementSummary());
            finalAssessment.setBehavioralHabitsSummary(aiGeneratedAssessment.getBehavioralHabitsSummary());
            finalAssessment.setCareerSuggestions(aiGeneratedAssessment.getCareerSuggestions());
            finalAssessment.setSuitablePositions(aiGeneratedAssessment.getSuitablePositions());
            finalAssessment.setPositionFitAnalysis(aiGeneratedAssessment.getPositionFitAnalysis());

        }

        finalAssessment.setTotalInterviewsAnalyzed(totalInterviews);

        if (avgOverallScore != null) {
            finalAssessment.setAverageOverallScore(BigDecimal.valueOf(avgOverallScore).setScale(2, RoundingMode.HALF_UP));
        } else {
            finalAssessment.setAverageOverallScore(BigDecimal.ZERO);
        }

        finalAssessment.setCompetencyRadarData(JSON.toJSONString(avgScores));

        if (finalAssessment.getId() == null) {
            careerAssessmentMapper.insert(finalAssessment);
        } else {
            careerAssessmentMapper.updateById(finalAssessment);
        }

        // **[CRITICAL FIX]** Re-fetch the object to get DB-generated values like timestamps.
        return careerAssessmentMapper.selectById(finalAssessment.getId());
    }
    /**
     * 一个简单的工具类，用于从可能包含额外文本的字符串中提取出干净的JSON部分。
     */
    public class JsonExtractor {
        public static String extractJson(String text) {
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            // 寻找第一个 '{' 或 '['
            int firstBracket = -1;
            int firstBrace = text.indexOf('{');
            int firstSquare = text.indexOf('[');

            if (firstBrace != -1 && firstSquare != -1) {
                firstBracket = Math.min(firstBrace, firstSquare);
            } else if (firstBrace != -1) {
                firstBracket = firstBrace;
            } else {
                firstBracket = firstSquare;
            }

            if (firstBracket == -1) {
                return null;
            }

            // 寻找最后一个 '}' 或 ']'
            int lastBracket = -1;
            int lastBrace = text.lastIndexOf('}');
            int lastSquare = text.lastIndexOf(']');

            if (lastBrace != -1 && lastSquare != -1) {
                lastBracket = Math.max(lastBrace, lastSquare);
            } else if (lastBrace != -1) {
                lastBracket = lastBrace;
            } else {
                lastBracket = lastSquare;
            }

            if (lastBracket != -1 && lastBracket > firstBracket) {
                return text.substring(firstBracket, lastBracket + 1);
            }

            return null;
        }
    }
    /**
     * **[FIXED]** This method is now more robust against null timestamp values.
     */
    private CareerProfileVo buildCareerProfileVo(CareerAssessment assessment) {
        Users user = userMapper.selectById(assessment.getUserId());

        CareerProfileVo vo = new CareerProfileVo();
        vo.setUserId(assessment.getUserId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setTotalInterviews(assessment.getTotalInterviewsAnalyzed());

        if (assessment.getAverageOverallScore() != null) {
            vo.setAverageOverallScore(assessment.getAverageOverallScore().doubleValue());
        } else {
            vo.setAverageOverallScore(0.0);
        }

        // **[ROBUSTNESS FIX]** Handle potential null timestamps gracefully.
        if (assessment.getUpdatedAt() != null) {
            vo.setGeneratedAt(assessment.getUpdatedAt().toString());
        } else if (assessment.getCreatedAt() != null) {
            vo.setGeneratedAt(assessment.getCreatedAt().toString());
        }

        Map<String, Double> radarDataMap = JSON.parseObject(assessment.getCompetencyRadarData(), new com.alibaba.fastjson.TypeReference<Map<String, Double>>() {});
        CareerProfileVo.RadarChartData radarChart = new CareerProfileVo.RadarChartData();
        radarChart.setLabels(List.of("专业知识", "技能匹配", "语言表达", "逻辑思维", "创新能力", "应变抗压"));
        radarChart.setValues(radarChart.getLabels().stream()
                .map(label -> radarDataMap.get(untranslateLabel(label)))
                .collect(Collectors.toList()));
        vo.setCompetencyRadarChart(radarChart);

        CareerProfileVo.AiAnalysis aiAnalysis = new CareerProfileVo.AiAnalysis();
        aiAnalysis.setStrengths(assessment.getStrengthsSummary());
        aiAnalysis.setPositionFitAnalysis(assessment.getPositionFitAnalysis());
        aiAnalysis.setAreasForImprovement(assessment.getImprovementSummary());
        aiAnalysis.setBehavioralHabits(assessment.getBehavioralHabitsSummary());
        aiAnalysis.setCareerDevelopmentSuggestions(assessment.getCareerSuggestions());
        aiAnalysis.setSuitablePositions(assessment.getSuitablePositions());
        vo.setAiAnalysis(aiAnalysis);

        return vo;
    }

    private String untranslateLabel(String label) {
        switch (label) {
            case "专业知识": return "professional_knowledge";
            case "技能匹配": return "skill_match";
            case "语言表达": return "verbal_expression";
            case "逻辑思维": return "logical_thinking";
            case "创新能力": return "innovation";
            case "应变抗压": return "stress_resistance";
            default: return "";
        }
    }

}