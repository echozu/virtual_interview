package com.echo.virtual_interview.controller;

import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.context.UserIdContext;

import com.echo.virtual_interview.model.dto.interview.andriod.ResumeData;
import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import com.echo.virtual_interview.service.IResumeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 简历接口
 */
@RestController
@RequestMapping("/api/resume")
@Slf4j
public class ResumeController {

    @Resource
    private IResumeService resumeService;

    /**
     * 前端-保存或更新简历
     * @param resumeDataDto 简历的完整数据
     * @return 操作成功标识
     */
    @PostMapping("/web/save")
    public BaseResponse<Boolean> webSaveOrUpdateResume(@RequestBody ResumeDataDto resumeDataDto) {

        
        Integer userId = UserIdContext.getUserIdContext();
        resumeService.saveOrUpdateResume(resumeDataDto, userId);
        
        return ResultUtils.success(true);
    }

    /**
     * 前端-根据当前登录用户信息获取简历
     * @return 组装好的简历数据
     */
    @GetMapping("/web/get")
    public BaseResponse<ResumeDataDto> webGetResume() {

        
        Integer userId = UserIdContext.getUserIdContext();
        ResumeDataDto resumeDataDto = resumeService.getResumeByUserId(userId);

        return ResultUtils.success(resumeDataDto);
    }
/*
    */
/**
     * 安卓端-保存或更新简历
     * @param resumeData 安卓端简历的完整数据
     * @return 操作成功标识
     *//*

    @PostMapping("/android/save")
    public BaseResponse<Boolean> androidSaveOrUpdateResume(@RequestBody ResumeData resumeData) {
        Integer userId = UserIdContext.getUserIdContext();
        resumeService.androidSaveOrUpdateResume(resumeData, userId);
        return ResultUtils.success(true);
    }

    */
/**
     * 安卓端-根据当前登录用户信息获取简历
     * @return 组装好的安卓端简历数据
     *//*

    @GetMapping("/android/get")
    public BaseResponse<ResumeData> androidGetResume() {
        Integer userId = UserIdContext.getUserIdContext();
        ResumeData resumeData = resumeService.androidGetResumeByUserId(userId);
        return ResultUtils.success(resumeData);
    }
*/

}