package com.xzzn.pollux.config;

import com.xzzn.pollux.aop.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import javax.annotation.Resource;

/**
 * JWT 拦截器配置类
 *
 * @author xzzn
 */
@Configuration
public class JwtInterceptorConfig extends WebMvcConfigurationSupport {

    @Resource
    private JwtInterceptor jwtInterceptor;


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册拦截器,要声明拦截器对象和要拦截的请求
        registry.addInterceptor(jwtInterceptor)
                // 所有路径都被拦截
                .addPathPatterns("/**")
                // 白名单
                .excludePathPatterns("")
                .excludePathPatterns("/doc.html", "/webjars/**", "/v3/api-docs", "/swagger-resources/**")
                .excludePathPatterns("/user/register")
                .excludePathPatterns("/user/login")
                .excludePathPatterns("/user/login/code")
                .excludePathPatterns("/user/register/send-code")
                .excludePathPatterns("/user/login/send-code");


    }
}