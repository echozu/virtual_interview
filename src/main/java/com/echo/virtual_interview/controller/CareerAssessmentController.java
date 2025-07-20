package com.echo.virtual_interview.controller;

import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.model.dto.career.CareerProfileVo;
import com.echo.virtual_interview.service.CareerAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "职业评测管理")
@RestController
@RequestMapping("/api/career-assessment")
@RequiredArgsConstructor
public class CareerAssessmentController {

    private final CareerAssessmentService careerAssessmentService;

    @GetMapping("/my-profile")
    @Operation(summary = "获取我的综合职业评测报告")
    public BaseResponse<CareerProfileVo> getMyCareerProfile() {
        // 在实际项目中，用户ID通常从安全上下文（如Spring Security）中获取
        // Long currentUserId = SecurityUtils.getUserId();
        Integer userId = UserIdContext.getUserIdContext();
        CareerProfileVo careerProfile = careerAssessmentService.generateAndGetCareerProfile(Long.valueOf(userId));
        return ResultUtils.success(careerProfile);
    }
}