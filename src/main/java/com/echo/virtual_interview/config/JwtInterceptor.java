package com.echo.virtual_interview.config;

import com.echo.virtual_interview.context.UserIdContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.echo.virtual_interview.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器
 * 
 * @author echo
 * @description 拦截需要认证的请求，验证JWT Token
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的token
        String token = request.getHeader("Authorization");
        
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // 移除 "Bearer " 前缀
            
            // 验证token
            if (jwtUtils.validateToken(token) && !jwtUtils.isTokenExpired(token)) {
                // 从token中获取用户信息
                String userId = jwtUtils.getUserIdFromToken(token);
                String username = jwtUtils.getUsernameFromToken(token);
                
                // todo:将用户信息设置到请求属性中
                request.setAttribute("user_id", userId);
                request.setAttribute("username", username);
                
                // 设置到上下文中
                UserIdContext.setUserIdContext(Integer.valueOf(userId));

                return true;
            }
        }
        
        // token无效，返回401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未授权访问\",\"data\":null}");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清除上下文
        UserIdContext.clearUserIdContext();
    }
}
