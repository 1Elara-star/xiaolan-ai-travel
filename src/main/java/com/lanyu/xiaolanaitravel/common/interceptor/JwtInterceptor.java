package com.lanyu.xiaolanaitravel.common.interceptor;

import com.lanyu.xiaolanaitravel.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** JWT 登录认证拦截器。 */
@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
            return false;
        }
        String token = authorization.substring(7);
        if (!jwtUtil.validateToken(token)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "登录状态已失效，请重新登录");
            return false;
        }
        request.setAttribute("currentUserId", jwtUtil.getUserId(token));
        request.setAttribute("currentUsername", jwtUtil.getUsername(token));
        request.setAttribute("currentRole", jwtUtil.getRole(token));
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
