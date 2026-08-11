package com.lanyu.xiaolanaitravel.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 管理员权限拦截器，必须放在 JWT 拦截器之后执行。 */
@Component
public class AdminInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        Object role = request.getAttribute("currentRole");
        if (role == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
            return false;
        }
        if (!"ADMIN".equals(role.toString())) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "没有管理员权限");
            return false;
        }
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
