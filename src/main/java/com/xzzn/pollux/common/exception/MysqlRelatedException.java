package com.xzzn.pollux.common.exception;

import org.springframework.http.HttpStatus;

public class MysqlRelatedException extends Exception {
    public static class InsertRecordException extends BusinessException {
        public InsertRecordException(String msg)
        {
            super(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
        }
    }
}
