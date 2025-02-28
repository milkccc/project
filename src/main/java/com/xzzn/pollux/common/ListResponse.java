package com.xzzn.pollux.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class ListResponse<T extends Serializable> implements Serializable {

    private int code;

    private T data;

    private Long totalCount;

    private String message;


    public ListResponse(Integer code, T data, Long totalCount, String message){
        this.code = code;
        this.data = data;
        this.totalCount = totalCount;
        this.message = message;
    }
}
