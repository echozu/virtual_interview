package com.echo.virtual_interview.context;

/**
 * 用户ID上下文工具类
 * 
 * @author echo
 * @description 用于在当前线程中存储和获取用户ID
 */
public class UserIdContext {

    private static final ThreadLocal<Integer> USER_ID_CONTEXT = new ThreadLocal<>();

    /**
     * 设置用户ID到上下文
     */
    public static void setUserIdContext(Integer userId) {
        USER_ID_CONTEXT.set(userId);
    }

    /**
     * 从上下文获取用户ID
     */
    public static Integer getUserIdContext() {
        return USER_ID_CONTEXT.get();
    }

    /**
     * 清除上下文中的用户ID
     */
    public static void clearUserIdContext() {
        USER_ID_CONTEXT.remove();
    }
}
