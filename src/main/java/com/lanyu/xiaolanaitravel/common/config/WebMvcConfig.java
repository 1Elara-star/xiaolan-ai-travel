package com.lanyu.xiaolanaitravel.common.config;

import com.lanyu.xiaolanaitravel.common.interceptor.AdminInterceptor;
import com.lanyu.xiaolanaitravel.common.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring MVC 配置：注册权限拦截器并允许前端跨域访问。 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    public WebMvcConfig(JwtInterceptor jwtInterceptor, AdminInterceptor adminInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/user/me", "/user/me/**", "/user/profile", "/user/profile/**",
                        "/travel/plan", "/travel/plan/**", "/favorites", "/favorites/**",
                        "/travel/draft", "/travel/draft/**", "/memories", "/memories/**",
                        "/ai/test", "/admin", "/admin/**");
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/ai/test", "/admin", "/admin/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split("\\s*,\\s*"))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
