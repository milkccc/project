package com.xzzn.pollux.service;

import com.xzzn.pollux.common.enums.VerificationTypeEnum;
import com.xzzn.pollux.common.exception.BusinessException;
import com.xzzn.pollux.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

@Service
@Slf4j
public class SMSService {

    private final Random random = new Random();

    private static final String VERIFICATION_CODE_PREFIX = "PHONE:CODE:";

    private static final String REGISTER_PREFIX = "REGISTER:";

    private static final String LOGIN_PREFIX = "LOGIN:";

    private static final Map<VerificationTypeEnum, BiPredicate<String, Integer>> messageActions;

    static {
        messageActions = new EnumMap<>(VerificationTypeEnum.class);
        messageActions.put(VerificationTypeEnum.REGISTER, SMSService::sendRegisterMessage);
        messageActions.put(VerificationTypeEnum.LOGIN, SMSService::sendLoginMessage);
    }

    public boolean sendVerificationCode(String phoneNum, VerificationTypeEnum type) {
        int code = generateRandomCode();
        String typePrefix = getTypePrefix(type);
        boolean sendStatus = sendMessage(type, phoneNum, code);

        if (sendStatus) {
            RedisUtils.set(VERIFICATION_CODE_PREFIX + typePrefix + phoneNum, code, 600, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }

    private static Boolean sendLoginMessage(String phoneNum, Integer code) {
        log.debug("成功向 {} 发送登陆验证码, 验证码为: {}", phoneNum, code);
        return true;
    }

    private static Boolean sendRegisterMessage(String phoneNum, Integer code) {
        log.debug("成功向 {} 发送注册验证码, 验证码为: {}", phoneNum, code);
        return true;
    }

    public boolean validateVerificationCode(String phoneNum, VerificationTypeEnum type, int enteredCode) {
        String typePrefix = getTypePrefix(type);
        Object object = RedisUtils.get(VERIFICATION_CODE_PREFIX + typePrefix + phoneNum);
        if (object == null) {
            return false;
        }
        return (int) object == enteredCode;

    }

    private boolean sendMessage(VerificationTypeEnum type, String phoneNum, int code) {
        BiPredicate<String, Integer> messagePredicate = messageActions.get(type);
        if (messagePredicate == null) {
            throw new BusinessException(500, "无法发送此类型验证码");
        }

        return messagePredicate.test(phoneNum, code);
    }


    public int generateRandomCode() {
        return random.nextInt(9000) + 1000;
    }

    public String getTypePrefix(VerificationTypeEnum type) {
        switch (type) {
            case REGISTER:
                return REGISTER_PREFIX;
            case LOGIN:
                return LOGIN_PREFIX;
            default:
                throw new BusinessException(500, "无法发送此类型验证码");
        }
    }

}
