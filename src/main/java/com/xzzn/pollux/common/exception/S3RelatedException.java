package com.xzzn.pollux.common.exception;

import org.springframework.http.HttpStatus;

public class S3RelatedException extends Exception {
    public static class MinioException extends BusinessException {
        public MinioException(String msg)
        {
            super(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
        }
    }

    public static class UploadObjectException extends BusinessException {
        public UploadObjectException(String msg)
        {
            super(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
        }
    }

    public static class DownloadObjectException extends BusinessException {
        public DownloadObjectException(String msg)
        {
            super(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
        }
    }

    public static class DeleteObjectException extends BusinessException {
        public DeleteObjectException(String msg)
        {
            super(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
        }
    }
}
