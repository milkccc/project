package com.xzzn.pollux.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.exception.JwtVerificationException;
import com.xzzn.pollux.entity.User;
import com.xzzn.pollux.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 拦截器
 *
 * @author xzzn
 */
@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    @Value("${jwt.token-header}")
    private String tokenHeader;

    @Resource
    private JwtUtils jwtUtils;

    private static final String OPTIONS_METHOD = "OPTIONS";
    private static final String NULL_STRING = "null";

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             @NotNull Object handler) throws Exception {
        try {
            log.info("Intercepting request: " + request.getRequestURI());

            String method = request.getMethod();
            if (OPTIONS_METHOD.equals(method)) {
                return true;
            }

            // 从请求中获取令牌
            String token = request.getHeader(tokenHeader);
            if (NULL_STRING.equals(String.valueOf(token))) {
                throw new JwtVerificationException("token 为空");
            }

            // 验证token
            User user = jwtUtils.verifyJwtToken(token);
            if (user != null) {
                return true;
            } else {
                sendErrorResponse(response, "token 已过期,请重新登陆");
            }
        } catch (JwtVerificationException e) {
            log.error("JWT 拦截器错误: ", e);
            sendErrorResponse(response, e.getMessage());
        } catch (Exception e) {
            log.error("JWT 拦截器错误: ", e);
            sendErrorResponse(response, "token 验证失败 " + e.getMessage());
        }
        return false;
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws JwtVerificationException, IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("data", null);
            responseMap.put("message", message);
            responseMap.put("code", HttpStatus.FORBIDDEN.value());
            String jsonMap = new ObjectMapper().writeValueAsString(responseMap);
            writer.println(jsonMap);
        }
    }
}