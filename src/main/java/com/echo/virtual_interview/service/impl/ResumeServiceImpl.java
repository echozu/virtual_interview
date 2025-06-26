package com.echo.virtual_interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.echo.virtual_interview.adapater.ResumeAdapter;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.constant.ModuleTypeConstants;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.mapper.ResumeMapper;
import com.echo.virtual_interview.model.dto.interview.andriod.ResumeData;
import com.echo.virtual_interview.model.dto.resum.BasicInfoDto;
import com.echo.virtual_interview.model.dto.resum.BasicInfoItemDto;
import com.echo.virtual_interview.model.dto.resum.OtherInfoDto;
import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import com.echo.virtual_interview.model.entity.Resume;
import com.echo.virtual_interview.model.entity.ResumeModule;
import com.echo.virtual_interview.service.IResumeModuleService;
import com.echo.virtual_interview.service.IResumeService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * <p>
 * 简历主表 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-22
 */
@Service
public class ResumeServiceImpl extends ServiceImpl<ResumeMapper, Resume> implements IResumeService {

    @Resource
    private IResumeModuleService resumeModuleService;
    @Autowired
    private ResumeMapper resumeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateResume(ResumeDataDto resumeDataDto, Integer userId) {
        // 1. 处理主简历信息
        Resume resume = baseMapper.selectOne(new QueryWrapper<Resume>().eq("user_id", userId));
        if (resume == null) {
            resume = new Resume();
            resume.setUserId(userId.longValue());
        }
        BasicInfoDto basicInfo = resumeDataDto.getBasicInfo();
        BeanUtils.copyProperties(basicInfo, resume);
        this.saveOrUpdate(resume);
        Long resumeId = resume.getId();

        // 2. 清理旧的模块数据
        resumeModuleService.remove(new QueryWrapper<ResumeModule>().eq("resume_id", resumeId));

        // 3. 插入新的模块数据
        // 3.1 处理基础信息
        List<ResumeModule> modulesToInsert = new ArrayList<>();
        if (!CollectionUtils.isEmpty(basicInfo.getBasicInfos())) {
            AtomicInteger sortCounter = new AtomicInteger(0);
            basicInfo.getBasicInfos().forEach(item -> {
                ResumeModule module = new ResumeModule();
                module.setResumeId(resumeId);
                module.setParentId(0L);
                // 【修改点】使用字符串常量赋值
                module.setModuleType(ModuleTypeConstants.BASIC_INFO);
                module.setSort(sortCounter.getAndIncrement());
                module.setItemKey(item.getBasicKey());
                module.setItemValue(item.getBasicVal());
                module.setIsAppear(true);
                modulesToInsert.add(module);
            });
        }
        if (!modulesToInsert.isEmpty()) {
            resumeModuleService.saveBatch(modulesToInsert);
        }

        // 3.2 递归处理其他模块信息
        if (!CollectionUtils.isEmpty(resumeDataDto.getOtherInfos())) {
            processAndInsertOtherInfos(resumeDataDto.getOtherInfos(), resumeId, 0L);
        }
    }

    private void processAndInsertOtherInfos(List<OtherInfoDto> dtoList, Long resumeId, Long parentId) {
        for (OtherInfoDto dto : dtoList) {
            ResumeModule module = new ResumeModule();
            BeanUtils.copyProperties(dto, module);
            module.setResumeId(resumeId);
            module.setParentId(parentId);
            // 【修改点】determineModuleType 方法现在返回 String
            module.setModuleType(determineModuleType(dto.getTitle()));
            resumeModuleService.save(module);

            if (!CollectionUtils.isEmpty(dto.getChildren())) {
                processAndInsertOtherInfos(dto.getChildren(), resumeId, module.getId());
            }
        }
    }

    @Override
    public ResumeDataDto getResumeByUserId(Integer userId) {
        Resume resume = baseMapper.selectOne(new QueryWrapper<Resume>().eq("user_id", userId));
        if (resume == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户简历不存在");
        }

        List<ResumeModule> allModules = resumeModuleService.list(new QueryWrapper<ResumeModule>()
                .eq("resume_id", resume.getId()));

        ResumeDataDto resumeDataDto = new ResumeDataDto();
        BasicInfoDto basicInfoDto = new BasicInfoDto();
        BeanUtils.copyProperties(resume, basicInfoDto);

        // 组装 BasicInfo
        List<BasicInfoItemDto> basicInfoItems = allModules.stream()
                // 【修改点】使用 .equals() 进行字符串比较
                .filter(m -> ModuleTypeConstants.BASIC_INFO.equals(m.getModuleType()))
                .sorted(Comparator.comparing(ResumeModule::getSort))
                .map(module -> {
                    BasicInfoItemDto itemDto = new BasicInfoItemDto();
                    itemDto.setId(UUID.randomUUID().toString());
                    itemDto.setBasicKey(module.getItemKey());
                    itemDto.setBasicVal(module.getItemValue());
                    return itemDto;
                }).collect(Collectors.toList());
        basicInfoDto.setBasicInfos(basicInfoItems);
        resumeDataDto.setBasicInfo(basicInfoDto);

        // 组装 OtherInfos (无Map实现)
        List<ResumeModule> otherModuleList = allModules.stream()
                // 【修改点】使用 .equals() 进行字符串比较
                .filter(m -> !ModuleTypeConstants.BASIC_INFO.equals(m.getModuleType()))
                .collect(Collectors.toList());
        
        List<OtherInfoDto> otherInfos = buildTree(0L, otherModuleList);
        resumeDataDto.setOtherInfos(otherInfos);

        return resumeDataDto;
    }

    private List<OtherInfoDto> buildTree(Long parentId, List<ResumeModule> allModules) {
        List<ResumeModule> children = allModules.stream()
                .filter(module -> parentId.equals(module.getParentId()))
                .sorted(Comparator.comparing(ResumeModule::getSort))
                .collect(Collectors.toList());

        return children.stream()
                .map(module -> {
                    OtherInfoDto dto = new OtherInfoDto();
                    BeanUtils.copyProperties(module, dto);
                    dto.setId(UUID.randomUUID().toString());
                    dto.setChildren(buildTree(module.getId(), allModules));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 【修改点】此工具方法现在返回 String 类型
     */
    private String determineModuleType(String title) {
        if (title == null) return ModuleTypeConstants.PROJECT;
        if (title.contains("教育经历")) return ModuleTypeConstants.EDUCATION;
        if (title.contains("相关技能")) return ModuleTypeConstants.SKILLS;
        if (title.contains("项目经历")) return ModuleTypeConstants.PROJECT;
        if (title.contains("荣誉证书")) return ModuleTypeConstants.HONOR;
        if (title.contains("个人评价")) return ModuleTypeConstants.EVALUATION;
        return ModuleTypeConstants.PROJECT;
    }
    public Long getResumeIdByUserId(Integer userId) {
        Resume resume = resumeMapper.selectOne(
                new QueryWrapper<Resume>().lambda().eq(Resume::getUserId, userId)
        );
        return resume == null ? null : resume.getId();
    }



        /**
         * 安卓端-保存或更新简历
         *
         * @param androidResumeData 安卓端简历的完整数据
         * @param userId            当前登录用户ID
         */
    @Override
    public void androidSaveOrUpdateResume(ResumeData androidResumeData, Integer userId) {
        // 1. 调用适配器，将安卓 DTO 转换为 Web 通用 DTO
        ResumeDataDto webDto = ResumeAdapter.convertToWebDto(androidResumeData);

        // 2. 调用已有的核心服务，完成数据库操作
        this.saveOrUpdateResume(webDto, userId);
    }

    /**
     * 安卓端-根据当前登录用户信息获取简历
     *
     * @param userId 当前登录用户ID
     * @return 组装好的安卓端简历数据
     */
    @Override
    public ResumeData androidGetResumeByUserId(Integer userId) {
        // 1. 调用核心服务，获取 Web 通用 DTO
        ResumeDataDto webDto = getResumeByUserId(userId);
        if (webDto == null) {
            return new ResumeData(); // 或者根据业务需求返回 null 或抛出异常
        }

        // 2. 调用适配器，将 Web 通用 DTO 转换为安卓 DTO
        // 注意：如适配器中所述，此处的逆向转换可能不是100%完美的
        return ResumeAdapter.convertToAndroidDto(webDto);
    }
}