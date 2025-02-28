package com.xzzn.pollux.common.exception;

import org.springframework.http.HttpStatus;

public class FileRelatedException extends Exception {
    public static class FileParseException extends BusinessException {
        public FileParseException(String msg)
        {
            super(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
        }
    }
}