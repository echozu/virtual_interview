package com.echo.virtual_interview.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.echo.virtual_interview.model.dto.users.UserQueryRequest;
import com.echo.virtual_interview.model.entity.Users;
import com.baomidou.mybatisplus.extension.service.IService;
import com.echo.virtual_interview.model.vo.LoginUserVO;
import com.echo.virtual_interview.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * <p>
 * 存储平台的用户基本信息 服务类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
public interface IUsersService extends IService<Users> {

    /**
     * 用户注册
     *
     * @param userName   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userName, String userPassword, String checkPassword,String email);

    /**
     * 用户登录
     *

     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String username, String email,String password, HttpServletRequest request);


    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    Users getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    Users getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(Users user);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(Users user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(Users user);

    /**
     * 获取脱敏的用户信息
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<Users> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<Users> getQueryWrapper(UserQueryRequest userQueryRequest);
}
