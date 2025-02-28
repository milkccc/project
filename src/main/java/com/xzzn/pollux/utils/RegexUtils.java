package com.xzzn.pollux.utils;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.xzzn.pollux.common.constant.RegexPatternsConstant;

/**
 * 正则工具类
 *
 * @author xzzn
 */

public class RegexUtils {

    private RegexUtils(){}
    /**
     * 是否是无效手机格式
     *
     * @param phone 要校验的手机号
     * @return true:符合,false:不符合
     */
    public static boolean isPhoneInvalid(String phone) {
        return mismatch(phone, RegexPatternsConstant.PHONE_REGEX);
    }

    /**
     * 是否是无效邮箱格式
     *
     * @param email 要校验的邮箱
     * @return true:符合,false:不符合
     */
    public static boolean isEmailInvalid(String email) {
        return mismatch(email, RegexPatternsConstant.EMAIL_REGEX);
    }

    /**
     * 是否是无效验证码格式
     *
     * @param code 要校验的验证码
     * @return true:符合,false:不符合
     */
    public static boolean isCodeInvalid(String code) {
        return mismatch(code, RegexPatternsConstant.VERIFY_CODE_REGEX);
    }

    /**
     * 校验是否不符合正则格式
     * 
     * @param str 字符串
     * @param regex 正则匹配规则
     * @return true:不符合,false:符合
     */
    private static boolean mismatch(String str, String regex) {
        if (StringUtils.isBlank(str)) {
            return true;
        }
        return !str.matches(regex);
    }
}

