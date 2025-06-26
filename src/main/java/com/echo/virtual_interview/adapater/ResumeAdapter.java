package com.echo.virtual_interview.adapater;

import com.echo.virtual_interview.model.dto.interview.andriod.*;
import com.echo.virtual_interview.model.dto.resum.BasicInfoDto;
import com.echo.virtual_interview.model.dto.resum.BasicInfoItemDto;
import com.echo.virtual_interview.model.dto.resum.OtherInfoDto;
import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 简历数据适配器
 * 用于在安卓端 DTO 和 Web端通用 DTO 之间进行转换
 */
public class ResumeAdapter {

    // 为了解析时定位关键信息，定义常量
    private static final String CITIES_PREFIX = "期望城市: ";
    private static final String SALARY_PREFIX = "薪资范围: ";
    private static final String DEGREE_PREFIX = "学历: ";
    private static final String CAMPUS_EXP_PREFIX = "在校经历: ";
    private static final String WORK_CONTENT_PREFIX = "工作内容: ";
    private static final String ACHIEVEMENTS_PREFIX = "主要业绩: ";
    private static final String DESC_PREFIX = "项目描述: ";
    private static final String PROJECT_ACHIEVEMENTS_PREFIX = "个人成就: ";
    private static final String LINKS_PREFIX = "项目链接: ";


    /**
     * 将安卓端的简历数据模型转换为Web端的通用数据模型
     * @param androidResume 安卓端发来的简历数据
     * @return Web端通用的简历数据
     */
    public static ResumeDataDto convertToWebDto(ResumeData androidResume) {
        ResumeDataDto webDto = new ResumeDataDto();
        ResumeBasicInfo androidBasicInfo = androidResume.getBasicInfo();

        // 1. 转换基础信息 (BasicInfo)
        BasicInfoDto webBasicInfo = new BasicInfoDto();
        if (androidBasicInfo != null) {
            webBasicInfo.setTitle(androidBasicInfo.getTitle());
            webBasicInfo.setCover(androidBasicInfo.getCover());
            webBasicInfo.setTag(androidBasicInfo.getTag());
            webBasicInfo.setAvatar(androidBasicInfo.getAvatar());

            List<BasicInfoItemDto> basicItems = new ArrayList<>();
            addBasicItem(basicItems, "姓名", androidBasicInfo.getName());
            addBasicItem(basicItems, "性别", androidBasicInfo.getGender());
            addBasicItem(basicItems, "年龄", androidBasicInfo.getAge());
            addBasicItem(basicItems, "学历", androidBasicInfo.getEducation());
            addBasicItem(basicItems, "毕业年份", androidBasicInfo.getGraduationYear());
            addBasicItem(basicItems, "电话", androidBasicInfo.getPhone());
            addBasicItem(basicItems, "邮箱", androidBasicInfo.getEmail());
            addBasicItem(basicItems, "微信", androidBasicInfo.getWechat());
            addBasicItem(basicItems, "身份", androidBasicInfo.getIdentity());
            addBasicItem(basicItems, "出生日期", androidBasicInfo.getBirthDate());
            addBasicItem(basicItems, "籍贯", androidBasicInfo.getBirthPlace());
            addBasicItem(basicItems, "求职状态", androidResume.getJobStatus());
            webBasicInfo.setBasicInfos(basicItems);
        }
        webDto.setBasicInfo(webBasicInfo);


        // 2. 转换其他模块信息 (OtherInfos)
        List<OtherInfoDto> otherInfos = new ArrayList<>();
        AtomicInteger sortCounter = new AtomicInteger(0);

        // 2.1 求职期望 (扁平结构)
        if (!CollectionUtils.isEmpty(androidResume.getJobExpectations())) {
             OtherInfoDto jobParent = createParentModule("求职期望", sortCounter.getAndIncrement());
             jobParent.setChildren(androidResume.getJobExpectations().stream().map(exp -> {
                OtherInfoDto dto = new OtherInfoDto();
                dto.setId(StringUtils.hasText(exp.getId()) ? exp.getId() : UUID.randomUUID().toString());
                dto.setSubject(exp.getPosition()); // 期望岗位
                dto.setMajor(exp.getIndustry()); // 期望行业
                String content = String.format("%s%s\n%s%s",
                        CITIES_PREFIX, exp.getCities() != null ? String.join(", ", exp.getCities()) : "",
                        SALARY_PREFIX, exp.getSalary());
                dto.setContent(content);
                dto.setIsAppear(true);
                 return dto;
             }).collect(Collectors.toList()));
             otherInfos.add(jobParent);
        }

        // 2.2 教育经历
        if (!CollectionUtils.isEmpty(androidResume.getEducationExperiences())) {
            OtherInfoDto educationParent = createParentModule("教育经历", sortCounter.getAndIncrement());
            educationParent.setChildren(androidResume.getEducationExperiences().stream().map(edu -> {
                OtherInfoDto dto = new OtherInfoDto();
                dto.setId(StringUtils.hasText(edu.getId()) ? edu.getId() : UUID.randomUUID().toString());
                dto.setSubject(edu.getSchool()); // 学校
                dto.setMajor(edu.getMajor()); // 专业
                dto.setFromDate(edu.getStartDate());
                dto.setToDate(edu.getEndDate());
                dto.setContent(String.format("%s%s\n%s%s",
                        DEGREE_PREFIX, edu.getDegree(),
                        CAMPUS_EXP_PREFIX, edu.getCampusExperience()));
                dto.setIsAppear(true);
                return dto;
            }).collect(Collectors.toList()));
            otherInfos.add(educationParent);
        }

        // 2.3 工作/实习经历
        if (!CollectionUtils.isEmpty(androidResume.getWorkExperiences())) {
            String title = androidResume.getWorkExperiences().stream().anyMatch(w -> w.getIsInternship() != null && w.getIsInternship()) ? "实习经历" : "工作经历";
            OtherInfoDto workParent = createParentModule(title, sortCounter.getAndIncrement());
            workParent.setChildren(androidResume.getWorkExperiences().stream().map(work -> {
                OtherInfoDto dto = new OtherInfoDto();
                dto.setId(StringUtils.hasText(work.getId()) ? work.getId() : UUID.randomUUID().toString());
                dto.setSubject(work.getCompanyName()); // 公司
                dto.setMajor(work.getPosition()); // 职位
                dto.setFromDate(work.getStartDate());
                dto.setToDate(work.getEndDate());
                dto.setToNow(work.getIsCurrentJob());
                dto.setContent(String.format("%s%s\n%s%s",
                       WORK_CONTENT_PREFIX, work.getWorkContent(),
                       ACHIEVEMENTS_PREFIX, work.getAchievements()));
                dto.setIsAppear(true);
                return dto;
            }).collect(Collectors.toList()));
            otherInfos.add(workParent);
        }

        // 2.4 项目经历
        if (!CollectionUtils.isEmpty(androidResume.getProjectExperiences())) {
            OtherInfoDto projectParent = createParentModule("项目经历", sortCounter.getAndIncrement());
            projectParent.setChildren(androidResume.getProjectExperiences().stream().map(proj -> {
                OtherInfoDto dto = new OtherInfoDto();
                dto.setId(StringUtils.hasText(proj.getId()) ? proj.getId() : UUID.randomUUID().toString());
                dto.setSubject(proj.getProjectName()); // 项目名称
                dto.setMajor(proj.getRole()); // 担任角色
                dto.setFromDate(proj.getStartDate());
                dto.setToDate(proj.getEndDate());
                dto.setContent(String.format("%s%s\n%s%s\n%s%s",
                        DESC_PREFIX, proj.getDescription(),
                        PROJECT_ACHIEVEMENTS_PREFIX, proj.getAchievements(),
                        LINKS_PREFIX, proj.getProjectLinks()));
                dto.setIsAppear(true);
                return dto;
            }).collect(Collectors.toList()));
            otherInfos.add(projectParent);
        }

        // 2.5 个人优势/评价
        if (androidResume.getPersonalAdvantage() != null && StringUtils.hasText(androidResume.getPersonalAdvantage().getContent())) {
            OtherInfoDto advantageDto = createParentModule("个人评价", sortCounter.getAndIncrement());
            advantageDto.setContent(androidResume.getPersonalAdvantage().getContent());
            otherInfos.add(advantageDto);
        }

        webDto.setOtherInfos(otherInfos);
        return webDto;
    }

    /**
     * 将Web端的通用数据模型转换为安卓端的简历数据模型 (完整实现)
     * @param webDto Web端通用的简历数据
     * @return 安卓端的简历数据
     */
    public static ResumeData convertToAndroidDto(ResumeDataDto webDto) {
        ResumeData androidResume = new ResumeData();
        androidResume.setJobExpectations(new ArrayList<>());
        androidResume.setEducationExperiences(new ArrayList<>());
        androidResume.setWorkExperiences(new ArrayList<>());
        androidResume.setProjectExperiences(new ArrayList<>());

        // 1. 转换基础信息
        if (webDto.getBasicInfo() != null) {
            ResumeBasicInfo androidBasicInfo = new ResumeBasicInfo();
            androidBasicInfo.setTitle(webDto.getBasicInfo().getTitle());
            androidBasicInfo.setCover(webDto.getBasicInfo().getCover());
            androidBasicInfo.setTag(webDto.getBasicInfo().getTag());
            androidBasicInfo.setAvatar(webDto.getBasicInfo().getAvatar());

            if (!CollectionUtils.isEmpty(webDto.getBasicInfo().getBasicInfos())) {
                for (BasicInfoItemDto item : webDto.getBasicInfo().getBasicInfos()) {
                    if (item == null || !StringUtils.hasText(item.getBasicKey())) continue;
                    switch (item.getBasicKey()) {
                        case "姓名": androidBasicInfo.setName(item.getBasicVal()); break;
                        case "性别": androidBasicInfo.setGender(item.getBasicVal()); break;
                        case "年龄": androidBasicInfo.setAge(item.getBasicVal()); break;
                        case "学历": androidBasicInfo.setEducation(item.getBasicVal()); break;
                        case "毕业年份": androidBasicInfo.setGraduationYear(item.getBasicVal()); break;
                        case "电话": androidBasicInfo.setPhone(item.getBasicVal()); break;
                        case "邮箱": androidBasicInfo.setEmail(item.getBasicVal()); break;
                        case "微信": androidBasicInfo.setWechat(item.getBasicVal()); break;
                        case "身份": androidBasicInfo.setIdentity(item.getBasicVal()); break;
                        case "出生日期": androidBasicInfo.setBirthDate(item.getBasicVal()); break;
                        case "籍贯": androidBasicInfo.setBirthPlace(item.getBasicVal()); break;
                        case "求职状态": androidResume.setJobStatus(item.getBasicVal()); break;
                    }
                }
            }
            androidResume.setBasicInfo(androidBasicInfo);
        }

        // 2. 转换其他模块
        if (!CollectionUtils.isEmpty(webDto.getOtherInfos())) {
            for (OtherInfoDto parentInfo : webDto.getOtherInfos()) {
                if (parentInfo == null) continue;
                String title = parentInfo.getTitle();
                if (!StringUtils.hasText(title)) continue;
                
                // 跳过没有子项的父模块（个人评价除外）
                if (CollectionUtils.isEmpty(parentInfo.getChildren()) && !title.contains("个人评价")) {
                     continue;
                }

                if (title.contains("求职期望")) {
                    parentInfo.getChildren().forEach(child -> {
                        JobExpectation exp = new JobExpectation();
                        exp.setId(child.getId());
                        exp.setPosition(child.getSubject());
                        exp.setIndustry(child.getMajor());
                        if (StringUtils.hasText(child.getContent())) {
                            exp.setCities(parseListFromString(child.getContent(), CITIES_PREFIX));
                            exp.setSalary(parseLineFromString(child.getContent(), SALARY_PREFIX));
                        }
                        androidResume.getJobExpectations().add(exp);
                    });

                } else if (title.contains("教育经历")) {
                    parentInfo.getChildren().forEach(child -> {
                        EducationExperience edu = new EducationExperience();
                        edu.setId(child.getId());
                        edu.setSchool(child.getSubject());
                        edu.setMajor(child.getMajor());
                        edu.setStartDate(child.getFromDate());
                        edu.setEndDate(child.getToDate());
                        if (StringUtils.hasText(child.getContent())) {
                            edu.setDegree(parseLineFromString(child.getContent(), DEGREE_PREFIX));
                            edu.setCampusExperience(parseLineFromString(child.getContent(), CAMPUS_EXP_PREFIX));
                        }
                        androidResume.getEducationExperiences().add(edu);
                    });
                } else if (title.contains("工作经历") || title.contains("实习经历")) {
                    parentInfo.getChildren().forEach(child -> {
                        WorkExperience work = new WorkExperience();
                        work.setId(child.getId());
                        work.setCompanyName(child.getSubject());
                        work.setPosition(child.getMajor());
                        work.setStartDate(child.getFromDate());
                        work.setEndDate(child.getToDate());
                        work.setIsCurrentJob(child.getToNow());
                        work.setIsInternship(title.contains("实习经历"));
                        if (StringUtils.hasText(child.getContent())) {
                           work.setWorkContent(parseLineFromString(child.getContent(), WORK_CONTENT_PREFIX));
                           work.setAchievements(parseLineFromString(child.getContent(), ACHIEVEMENTS_PREFIX));
                        }
                        androidResume.getWorkExperiences().add(work);
                    });

                } else if (title.contains("项目经历")) {
                    parentInfo.getChildren().forEach(child -> {
                        ProjectExperience proj = new ProjectExperience();
                        proj.setId(child.getId());
                        proj.setProjectName(child.getSubject());
                        proj.setRole(child.getMajor());
                        proj.setStartDate(child.getFromDate());
                        proj.setEndDate(child.getToDate());
                        if (StringUtils.hasText(child.getContent())) {
                            proj.setDescription(parseLineFromString(child.getContent(), DESC_PREFIX));
                            proj.setAchievements(parseLineFromString(child.getContent(), PROJECT_ACHIEVEMENTS_PREFIX));
                            proj.setProjectLinks(parseLineFromString(child.getContent(), LINKS_PREFIX));
                        }
                        androidResume.getProjectExperiences().add(proj);
                    });
                } else if (title.contains("个人评价")) {
                    PersonalAdvantage advantage = new PersonalAdvantage();
                    advantage.setContent(parentInfo.getContent());
                    androidResume.setPersonalAdvantage(advantage);
                }
            }
        }

        return androidResume;
    }
    
    // ================== 辅助方法 ==================

    /**
     * 辅助方法：添加基础信息项，如果值不为空
     */
    private static void addBasicItem(List<BasicInfoItemDto> list, String key, String value) {
        if (StringUtils.hasText(value)) {
            BasicInfoItemDto item = new BasicInfoItemDto();
            item.setId(UUID.randomUUID().toString());
            item.setBasicKey(key);
            item.setBasicVal(value);
            list.add(item);
        }
    }

    /**
     * 辅助方法：创建一个父模块
     */
    private static OtherInfoDto createParentModule(String title, int sort) {
        OtherInfoDto parentDto = new OtherInfoDto();
        parentDto.setId(UUID.randomUUID().toString());
        parentDto.setTitle(title);
        parentDto.setSort(sort);
        parentDto.setIsAppear(true);
        parentDto.setChildren(new ArrayList<>());
        return parentDto;
    }

    /**
     * 辅助方法：从多行文本中根据前缀解析出对应行的数据
     * @param text 完整内容
     * @param prefix 行前缀，如 "薪资范围: "
     * @return 提取出的数据，找不到则返回空字符串
     */
    private static String parseLineFromString(String text, String prefix) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(prefix)) {
            return "";
        }
        for (String line : text.split("\n")) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    /**
     * 辅助方法：从多行文本中解析出城市列表
     * @param text 完整内容
     * @param prefix 行前缀，如 "期望城市: "
     * @return 城市列表，找不到则返回空列表
     */
    private static List<String> parseListFromString(String text, String prefix) {
        String line = parseLineFromString(text, prefix);
        if (StringUtils.hasText(line)) {
            return Arrays.stream(line.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}