package com.lanyu.xiaolanaitravel.common.interceptor;

import com.lanyu.xiaolanaitravel.common.exception.ApiErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 管理员权限拦截器，必须放在 JWT 拦截器之后执行。 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final ApiErrorWriter errorWriter;

    public AdminInterceptor(ApiErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Object role = request.getAttribute("currentRole");
        if (role == null) {
            errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
            return false;
        }
        if (!"ADMIN".equals(role.toString())) {
            errorWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN, "没有管理员权限");
            return false;
        }
        return true;
    }

}
