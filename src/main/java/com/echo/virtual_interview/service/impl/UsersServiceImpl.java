package com.echo.virtual_interview.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.mapper.ExperienceCommentsMapper;
import com.echo.virtual_interview.mapper.ExperiencePostsMapper;
import com.echo.virtual_interview.model.dto.users.ExperienceStatsDTO;
import com.echo.virtual_interview.model.dto.users.UserQueryRequest;
import com.echo.virtual_interview.model.entity.Users;
import com.echo.virtual_interview.mapper.UsersMapper;
import com.echo.virtual_interview.model.enums.UserRoleEnum;
import com.echo.virtual_interview.model.vo.LoginUserVO;
import com.echo.virtual_interview.model.vo.UserVO;
import com.echo.virtual_interview.service.IUsersService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.echo.virtual_interview.constant.UserConstant.USER_LOGIN_STATE;

/**
 * <p>
 * 存储平台的用户基本信息 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Service
@Slf4j
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements IUsersService {

    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "echo";
    @Resource
    private ExperiencePostsMapper postsMapper;
    @Resource
    private ExperienceCommentsMapper commentsMapper;

    @Override
    public long userRegister(String userName, String userPassword, String checkPassword,String email) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userName, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userName.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码不相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userName.intern()) {
            // 账户不能重复
            QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(Users::getUsername, userName);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
            }

            QueryWrapper<Users> queryWrapperEmail = new QueryWrapper<>();
            queryWrapperEmail.lambda().eq(Users::getEmail, email);
            long count2 = this.baseMapper.selectCount(queryWrapperEmail);
            if (count2 > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被注册");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            Users user = new Users();
            user.setUsername(userName);
            user.setPassword(encryptPassword);
            user.setEmail(email);
            user.setNickname(userName);
            //todo:默认图片
            user.setAvatarUrl("https://echo-interview.oss-cn-guangzhou.aliyuncs.com/interview/channel/b516870d-6281-4716-a615-e06bdae33d19.jpg");
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String usernameOrEmail, String userPassword, HttpServletRequest request) {
        // 1. 参数校验
        if (StringUtils.isBlank(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (StringUtils.isBlank(usernameOrEmail)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或邮箱不能为空");
        }

        // 2. 密码长度校验
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不能少于8位");
        }

        // 3. 密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 4. 先尝试作为用户名查询
        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(Users::getUsername, usernameOrEmail)
                .eq(Users::getPassword, encryptPassword);

        Users user = this.baseMapper.selectOne(queryWrapper);

        // 5. 如果用户名查询不到，尝试作为邮箱查询
        if (user == null) {
            queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda()
                    .eq(Users::getEmail, usernameOrEmail)
                    .eq(Users::getPassword, encryptPassword);
            user = this.baseMapper.selectOne(queryWrapper);
        }

        // 6. 如果两种方式都查询不到用户
        if (user == null) {
            log.info("user login failed, credentials not match");
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在或密码错误");
        }

        // 7. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @return
     */
    @Override
    public Users getLoginUser() {
        Integer userId = UserIdContext.getUserIdContext();
        Users currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public Users getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        Users currentUser = (Users) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        Users user = (Users) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(Users user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(Users user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(Users user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<Users> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<Users> getQueryWrapper(UserQueryRequest userQueryRequest) {
        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
/*        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);*/
        return queryWrapper;
    }

    /**
     * 获取指定用户的面经相关统计数据。
     * 1. 从 experience_posts 表中聚合查询用户获得的总点赞数、总收藏数以及发布的总面经数。
     * 2. 从 experience_comments 表中查询用户所有面经收到的总评论数。
     * 3. 将这些数据整合到 ExperienceStatsDTO 中并返回。
     *
     * @param userId 要查询的用户ID
     * @return 包含统计数据的DTO对象
     */
    @Override
    public ExperienceStatsDTO getExperienceStatsByUserId(Long userId) {
        // 1. 获取点赞、收藏、面经数
        ExperienceStatsDTO statsDTO = postsMapper.selectUserExperienceStats(userId);

        // 2. 获取评论数
        Long totalComments = commentsMapper.countTotalCommentsByUserId(userId);
        statsDTO.setTotalComments(totalComments);

        return statsDTO;
    }


}
