package com.xzzn.pollux.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzzn.pollux.entity.UserLogin;
import com.xzzn.pollux.mapper.UserLoginMapper;
import com.xzzn.pollux.service.IUserLoginService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户登陆 服务实现类
 * </p>
 *
 * @author xzzn
 */
@Service
public class UserLoginServiceImpl extends ServiceImpl<UserLoginMapper, UserLogin> implements IUserLoginService {

}
