package com.xzzn.pollux.common.exception;

import lombok.Getter;

@Getter
public class FileProcessingException extends Exception {
    private final String fileId;

    public FileProcessingException(String message, String fileId) {
        super(message);
        this.fileId = fileId;
    }
}
