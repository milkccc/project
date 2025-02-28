package com.xzzn.pollux.aop;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.xzzn.pollux.annotation.AuthCheck;
import com.xzzn.pollux.common.enums.ErrorCodeEnum;
import com.xzzn.pollux.common.exception.BusinessException;
import com.xzzn.pollux.entity.User;
import com.xzzn.pollux.utils.JwtUtils;
import io.jsonwebtoken.SignatureException;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 权限校验 AOP
 *
 * @author xzzn
 */
@Aspect
@Component
public class AuthInterceptor {

    @Value("${jwt.token-header}")
    private String tokenHeader;

    @Resource
    private JwtUtils jwtUtils;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        List<String> anyRole = Arrays.stream(authCheck.anyRole()).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 获取当前登录用户,token是无状态的,需要解析token获取对应的User
        String token = request.getHeader(tokenHeader);
        String nullStr = "null";
        if (nullStr.equals(String.valueOf(token))) {
            throw new SignatureException("token为空");
        }

        // 解析验证token获取User
        User user = jwtUtils.verifyJwtToken(token);

        // 拥有任意权限即通过
        if (CollectionUtils.isNotEmpty(anyRole)) {
            String userRole = user.getUserRole();
            if (!anyRole.contains(userRole)) {
                throw new BusinessException(ErrorCodeEnum.NO_AUTH_ERROR);
            }
        }
        // 必须有所有权限才通过
        if (StringUtils.isNotBlank(mustRole)) {
            String userRole = user.getUserRole();
            if (!mustRole.equals(userRole)) {
                throw new BusinessException(ErrorCodeEnum.NO_AUTH_ERROR);
            }
        }
        // 通过权限校验,放行
        return joinPoint.proceed();
    }
}

