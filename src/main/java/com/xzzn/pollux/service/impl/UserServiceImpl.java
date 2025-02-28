package com.xzzn.pollux.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzzn.pollux.common.enums.VerificationTypeEnum;
import com.xzzn.pollux.common.exception.BusinessException;
import com.xzzn.pollux.entity.User;
import com.xzzn.pollux.entity.UserLogin;
import com.xzzn.pollux.mapper.UserMapper;
import com.xzzn.pollux.model.dto.UserListPageDTO;
import com.xzzn.pollux.model.pojo.FingerPrint;
import com.xzzn.pollux.model.vo.request.user.UserLoginByCaptchaRequest;
import com.xzzn.pollux.model.vo.request.user.UserLoginRequest;
import com.xzzn.pollux.model.vo.request.user.UserRegisterRequest;
import com.xzzn.pollux.model.vo.response.user.UserInfo;
import com.xzzn.pollux.service.IUserService;
import com.xzzn.pollux.service.SMSService;
import com.xzzn.pollux.utils.JwtUtils;
import com.xzzn.pollux.utils.RedisUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author xzzn
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private UserLoginServiceImpl userLoginService;

    @Resource
    private SMSService smsService;

    @Resource
    private JwtUtils jwtUtils;

    @Value("${jwt.expire-time}")
    private int expireTime;

    private static final String REDIS_LOGIN_KEY = "REDIS:LOGIN:KEY:";

    private static final String SALT = "xzzn";

    final Object lockObject = new Object();

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 用户ID
     */
    public String userRegister(UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(500, "用户注册请求体为空");
        }
        String userAccount = userRegisterRequest.getAccount();
        String userPassword = userRegisterRequest.getPassword();
        String userName = userRegisterRequest.getUserName();
        int verificationCode = userRegisterRequest.getVerificationCode();

        if (!smsService.validateVerificationCode(userAccount, VerificationTypeEnum.REGISTER, verificationCode)) {
            throw new BusinessException(500, "验证码验证失败");
        }

        // 注册
        synchronized (lockObject) {
            Long count = this.lambdaQuery().eq(User::getUserAccount, userAccount).count();
            if (count > 0) {
                throw new BusinessException(500, "注册失败, 账号重复");
            }
            // 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            user.setUserName(userName);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(500, "注册失败, 数据库错误");
            }
            return user.getId();
        }
    }

    /**
     * 用户登陆
     *
     * @param userLoginRequest 用户登陆请求
     * @return token
     */
    public String userLogin(UserLoginRequest userLoginRequest) {
        // 校验
        if (userLoginRequest == null) {
            throw new BusinessException(500, "用户登陆请求体为空");
        }
        String userAccount = userLoginRequest.getAccount();
        String userPassword = userLoginRequest.getPassword();
        FingerPrint fingerPrint = userLoginRequest.getFingerPrint();
        if (StringUtils.isAnyBlank(userAccount, userPassword) || fingerPrint == null) {
            throw new BusinessException(500, "参数不能为空");
        }
        // 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询
        User user = this.lambdaQuery().eq(User::getUserAccount, userAccount)
                .eq(User::getUserPassword, encryptPassword).one();

        if (user == null) {
            throw new BusinessException(500, "用户不存在或密码错误");
        }
        // 获取token
        String token = getToken(user);

        // 登录信息存入数据库
        setUserLogin(user.getId(), fingerPrint);

        // 登录状态存入Redis
        RedisUtils.set(REDIS_LOGIN_KEY + user.getId(), user, expireTime, TimeUnit.MINUTES);

        return token;
    }

    /**
     * 用户验证码登陆
     *
     * @param userLoginByCaptchaRequest 用户验证码登陆
     * @return token
     */
    public String userLoginByCaptcha(UserLoginByCaptchaRequest userLoginByCaptchaRequest) {
        // 校验
        if (userLoginByCaptchaRequest == null) {
            throw new BusinessException(500, "用户验证码登陆请求体为空");
        }
        String userAccount = userLoginByCaptchaRequest.getAccount();
        int verificationCode = userLoginByCaptchaRequest.getVerificationCode();
        FingerPrint fingerPrint = userLoginByCaptchaRequest.getFingerPrint();
        // 判断验证码
        boolean verificationResult = smsService.validateVerificationCode(userAccount, VerificationTypeEnum.LOGIN, verificationCode);
        if (!verificationResult) {
            throw new BusinessException(500, "验证码错误");
        }

        User user = this.lambdaQuery().eq(User::getUserAccount, userAccount).one();
        if (user == null) {
            throw new BusinessException(500, "用户不存在");
        }
        // 获取token
        String token = getToken(user);

        // 登录信息存入数据库
        setUserLogin(user.getId(), fingerPrint);

        // 登录状态存入Redis
        RedisUtils.set(REDIS_LOGIN_KEY + user.getId(), user, expireTime, TimeUnit.MINUTES);

        return token;
    }

    private void setUserLogin(String userId, FingerPrint fingerPrint) {
        UserLogin userLogin = UserLogin.builder()
                .userId(userId)
                .loginTime(LocalDateTime.now())
                .loginIp(fingerPrint.getIp())
                .loginBrowser(fingerPrint.getBrowser())
                .loginOs(fingerPrint.getOs())
                .loginDevice(fingerPrint.getDevice())
                .loginStatus(1)
                .build();
        userLoginService.save(userLogin);
    }

    /**
     * 获取用户
     *
     * @param token token
     * @return 用户
     */
    public User getUser(String token) {
        return jwtUtils.verifyJwtToken(token);
    }

    /**
     * 获取用户信息
     *
     * @param token token
     * @return 用户
     */
    public UserInfo getUserInfoByToken(String token) {
        User user = jwtUtils.verifyJwtToken(token);
        if (user == null) {
            throw new BusinessException(500, "用户不存在");
        }
        return UserInfo.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .userAccount(user.getUserAccount())
                .gender(user.getGender())
                .userAvatar(user.getUserAvatar())
                .build();
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户id
     * @return 用户
     */
    public UserInfo getUserInfoById(String userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(500, "用户" + userId + "不存在");
        }
        return UserInfo.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .userAccount(user.getUserAccount())
                .gender(user.getGender())
                .userAvatar(user.getUserAvatar())
                .build();
    }


    /**
     * 分页返回所有用户
     *
     * @return 用户列表
     */
    public UserListPageDTO getAllUserByPage(Integer page, Integer size, String sortAttribute, String sortDirection) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        // 根据排序属性和排序方向来进行排序
        if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(sortAttribute) && com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(sortDirection)) {
            if ("asc".equalsIgnoreCase(sortDirection)) {
                queryWrapper.orderByAsc(com.baomidou.mybatisplus.core.toolkit.StringUtils.camelToUnderline(sortAttribute));
            } else if ("desc".equalsIgnoreCase(sortDirection)) {
                queryWrapper.orderByDesc(com.baomidou.mybatisplus.core.toolkit.StringUtils.camelToUnderline(sortAttribute));
            }
        }

        // 返回查询结果总数
        long totalCount = this.count(queryWrapper);
        List<User> records = this.page(new Page<>(page, size), queryWrapper).getRecords();
        // 根据page和size进行分页
        return new UserListPageDTO(totalCount, records);
    }

    /**
     * 根据用户id批量查询用户信息
     *
     * @param userIdList 用户id列表
     * @return 用户列表
     */
    public List<UserInfo> getUserInfoListByIdList(List<String> userIdList) {
        if (userIdList == null || userIdList.isEmpty()) {
            return new ArrayList<>();
        }
        List<User> userList = listByIds(userIdList);
        return userList.stream()
                .map(this::mapUserToUserInfo)
                .collect(Collectors.toList());
    }

    public String getToken(User user) {
        Map<String, Object> userInfoMap = new HashMap<>(3);
        userInfoMap.put("id", user.getId());
        userInfoMap.put("userAccount", user.getUserAccount());
        userInfoMap.put("UserName", user.getUserName());
        return jwtUtils.createJwtToken(userInfoMap);
    }

    private UserInfo mapUserToUserInfo(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .userAccount(user.getUserAccount())
                .userAvatar(user.getUserAvatar())
                .gender(user.getGender())
                .build();
    }

}
