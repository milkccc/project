package com.xzzn.pollux.model.pojo;

import com.xzzn.pollux.common.enums.FileTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 文件树节点
 *
 * @author xzzn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTreeNode implements Serializable {

    /**
     * 名称
     */
    private String name;

    /**
     * 类型
     */
    private FileTypeEnum type;

    /**
     * 文件id
     */
    private String fileId;

    /**
     * 子文件列表
     */
    private CopyOnWriteArrayList<FileTreeNode> children;

    public FileTreeNode(String name, FileTypeEnum type) {
        this.name = name;
        this.type = type;
        this.children = new CopyOnWriteArrayList<>();
    }
}
