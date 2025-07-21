package com.echo.virtual_interview.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;


import java.util.List;
import java.util.Map;


import com.echo.virtual_interview.annotation.AuthCheck;
import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.DeleteRequest;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.constant.UserConstant;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.exception.ThrowUtils;
import com.echo.virtual_interview.model.dto.users.*;
import com.echo.virtual_interview.model.entity.Users;
import com.echo.virtual_interview.model.vo.LoginResultVO;
import com.echo.virtual_interview.model.vo.LoginUserVO;
import com.echo.virtual_interview.model.vo.UserVO;
import com.echo.virtual_interview.service.IUsersService;
import com.echo.virtual_interview.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UsersController {

    @Resource
    private IUsersService userService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private JwtUtils jwtUtils;
    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 验证邮箱验证码
        String email = userRegisterRequest.getEmail();
        String inputCaptcha = userRegisterRequest.getCaptcha();

        // 从 Redis 获取验证码
        String storedCaptcha = redisTemplate.opsForValue().get("captcha:" + email);

        // 验证码校验
        if (storedCaptcha == null || !storedCaptcha.equals(inputCaptcha)) {
            return  ResultUtils.error(40000,"验证码错误");
        }

        String userName = userRegisterRequest.getUsername();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userName, userPassword, checkPassword)) {
            return null;
        }
        long result = userService.userRegister(userName, userPassword, checkPassword,email);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginResultVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
                                                 HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String password = userLoginRequest.getPassword();
        String usernameOrEmail = userLoginRequest.getUsernameOrEmail();

        // 验证密码不能为空
        if (StringUtils.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }

        // 验证用户名或邮箱不能为空
        if (StringUtils.isBlank(usernameOrEmail)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或邮箱不能为空");
        }
        // 调用服务层方法
        LoginUserVO loginUserVO = userService.userLogin(usernameOrEmail, password, request);
        String token = jwtUtils.generateToken(String.valueOf(loginUserVO.getId()), loginUserVO.getUsername());
        LoginResultVO loginResultVO = new LoginResultVO();
        loginResultVO.setToken(token);
        loginResultVO.setUserInfo(loginUserVO);
        return ResultUtils.success(loginResultVO);
    }

/*    *//**
     * 用户注销
     *
     * @param request
     * @return
     *//*
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }*/

    /**
     * 获取当前登录用户信息
     *
     * @param request
     * @return
     */
    @GetMapping("/get/message")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        Users user = userService.getLoginUser();
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest
     * @param request
     * @return
     */
    @PostMapping("/update/message")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
                                              HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Users loginUser = userService.getLoginUser();
        Users user = new Users();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        user.setId(loginUser.getId());
        boolean result = userService.updateById(user); //更新用户信息
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 获取指定用户的面经统计数据
     * @return ResponseEntity 包含 ExperienceStatsDTO
     */
    @GetMapping("/experience-stats")
    public BaseResponse<ExperienceStatsDTO> getUserExperienceStats() {
        Integer userId = UserIdContext.getUserIdContext();
        ExperienceStatsDTO stats = userService.getExperienceStatsByUserId(Long.valueOf(userId));
        return ResultUtils.success(stats);
    }
//    管理员相关的：创建用户、修改用户、删除用户等功能
//
//    /**
//     * 创建用户
//     *
//     * @param userAddRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/add")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
//        if (userAddRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User user = new User();
//        BeanUtils.copyProperties(userAddRequest, user);
//        // 默认密码 ***REMOVED***78
//        String defaultPassword = "***REMOVED***78";
//        String encryptPassword = DigestUtils.md5DigestAsHex((UserServiceImpl.SALT + defaultPassword).getBytes());
//        user.setUserPassword(encryptPassword);
//        boolean result = userService.save(user);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//        return ResultUtils.success(user.getId());
//    }
//
//    /**
//     * 删除用户
//     *
//     * @param deleteRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/delete")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
//        if (deleteRequest == null || deleteRequest.getId() <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        boolean b = userService.removeById(deleteRequest.getId());
//        return ResultUtils.success(b);
//    }
//
//    /**
//     * 更新用户
//     *
//     * @param userUpdateRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/update")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
//                                            HttpServletRequest request) {
//        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User user = new User();
//        BeanUtils.copyProperties(userUpdateRequest, user);
//        boolean result = userService.updateById(user);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//        return ResultUtils.success(true);
//    }
//
//    /**
//     * 根据 id 获取用户（仅管理员）
//     *
//     * @param id
//     * @param request
//     * @return
//     */
//    @GetMapping("/get")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<User> getUserById(long id, HttpServletRequest request) {
//        if (id <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User user = userService.getById(id);
//        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
//        return ResultUtils.success(user);
//    }
//
//    /**
//     * 根据 id 获取包装类
//     *
//     * @param id
//     * @param request
//     * @return
//     */
//    @GetMapping("/get/vo")
//    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
//        BaseResponse<User> response = getUserById(id, request);
//        User user = response.getData();
//        return ResultUtils.success(userService.getUserVO(user));
//    }
//
//    /**
//     * 分页获取用户列表（仅管理员）
//     *
//     * @param userQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/list/page")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
//                                                   HttpServletRequest request) {
//        long current = userQueryRequest.getCurrent();
//        long size = userQueryRequest.getPageSize();
//        Page<User> userPage = userService.page(new Page<>(current, size),
//                userService.getQueryWrapper(userQueryRequest));
//        return ResultUtils.success(userPage);
//    }
//
//    /**
//     * 分页获取用户封装列表
//     *
//     * @param userQueryRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/list/page/vo")
//    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
//                                                       HttpServletRequest request) {
//        if (userQueryRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        long current = userQueryRequest.getCurrent();
//        long size = userQueryRequest.getPageSize();
//        // 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        Page<User> userPage = userService.page(new Page<>(current, size),
//                userService.getQueryWrapper(userQueryRequest));
//        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
//        List<UserVO> userVO = userService.getUserVO(userPage.getRecords());
//        userVOPage.setRecords(userVO);
//        return ResultUtils.success(userVOPage);
//    }
}
