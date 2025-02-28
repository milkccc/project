package com.xzzn.pollux.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JWT 工具类
 *
 * @author xzzn
 */
@Slf4j
@Component
public class JwtUtils {

    private JwtUtils() {
    }


    @Value("${jwt.secret-key}")
    private String tokenSecret;

    @Value("${jwt.expire-time}")
    private long expireTime;

    private static final String REDIS_LOGIN_KEY = "REDIS:LOGIN:KEY:";

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 生成 JWT token
     *
     * @param map 存放 payload 信息
     */
    public String createJwtToken(Map<String, Object> map) {
        return Jwts.builder()
                .setClaims(map)
                //.setIssuedAt(new Date(System.currentTimeMillis()))
                //.setExpiration(new Date(System.currentTimeMillis() + expireTime * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, tokenSecret)
                .compact();
    }


    /**
     * 从令牌中获取 payload 部分存放的数据
     *
     * @param token 令牌
     */
    public Claims parseToken(String token) {
        log.debug(token);
        return Jwts.parser()
                .setSigningKey(tokenSecret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证用户信息
     *
     * @param token 令牌
     */
    public User verifyJwtToken(String token) {
        Claims claims = parseToken(token);
        String id = String.valueOf(claims.get("id"));
        Object userJson = RedisUtils.get(REDIS_LOGIN_KEY + id);

        if (userJson != null) {
            try {
                return objectMapper.readValue(userJson.toString(), User.class);
            } catch (IOException e) {
                log.error("转换User对象 {} 错误", userJson, e);
            }
        }
        return null;
    }


    /**
     * 刷新令牌时间,刷新redis缓存时间
     *
     * @param user 用户
     */
    public void refreshToken(User user) {
        // 重新设置User对象的过期时间,再刷新缓存
        RedisUtils.set(REDIS_LOGIN_KEY + user.getId(), user, (int) expireTime, TimeUnit.MINUTES);
    }

}