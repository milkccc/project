package com.xzzn.pollux.validator;

import com.xzzn.pollux.utils.RegexUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PhoneFormatValidator implements ConstraintValidator<PhoneFormat, String> {

    @Override
    public void initialize(PhoneFormat constraintAnnotation) {
        // 初始化方法
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && RegexUtils.isPhoneInvalid(value);
    }
}
