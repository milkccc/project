package com.xzzn.pollux.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 删除请求
 *
 * @author xzzn
 */
@Data
public class DeleteRequest implements Serializable {

    private List<String> deleteIds;

    private static final long serialVersionUID = 1L;
}