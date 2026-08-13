package com.lanyu.xiaolanaitravel.common.interceptor;

import com.lanyu.xiaolanaitravel.common.util.JwtUtil;
import com.lanyu.xiaolanaitravel.common.exception.ApiErrorWriter;
import com.lanyu.xiaolanaitravel.user.entity.User;
import com.lanyu.xiaolanaitravel.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** JWT 登录认证拦截器。 */
@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final ApiErrorWriter errorWriter;

    public JwtInterceptor(JwtUtil jwtUtil, UserMapper userMapper, ApiErrorWriter errorWriter) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.errorWriter = errorWriter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
            return false;
        }
        String token = authorization.substring(7).trim();
        Long userId;
        String username;
        try {
            Claims claims = jwtUtil.parseToken(token);
            Number claimUserId = claims.get("userId", Number.class);
            if (claimUserId == null || claims.getSubject() == null) {
                throw new IllegalArgumentException("missing identity claims");
            }
            userId = claimUserId.longValue();
            username = claims.getSubject();
        } catch (RuntimeException exception) {
            errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "登录状态已失效，请重新登录");
            return false;
        }

        // Resolve the current role and account state from the database. A deleted user or
        // revoked administrator token must not remain valid until the JWT expires.
        User user = userMapper.selectById(userId);
        if (user == null || !username.equals(user.getUsername())) {
            errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "账号已失效，请重新登录");
            return false;
        }
        request.setAttribute("currentUserId", user.getId());
        request.setAttribute("currentUsername", user.getUsername());
        request.setAttribute("currentRole", user.getRole());
        return true;
    }
}
