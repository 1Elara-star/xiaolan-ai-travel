package com.lanyu.xiaolanaitravel.common.config;

import com.lanyu.xiaolanaitravel.common.interceptor.AdminInterceptor;
import com.lanyu.xiaolanaitravel.common.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring MVC 配置：注册权限拦截器并允许前端跨域访问。 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;
    private final AdminInterceptor adminInterceptor;

    public WebMvcConfig(JwtInterceptor jwtInterceptor, AdminInterceptor adminInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/user/me", "/user/profile/**","/travel/plan/**", "/admin/**");
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
