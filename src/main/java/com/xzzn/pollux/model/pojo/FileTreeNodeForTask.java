package com.xzzn.pollux.model.pojo;

import com.xzzn.pollux.common.enums.FileTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 任务文件树节点
 *
 * @author xzzn
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileTreeNodeForTask implements Serializable {
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
    private CopyOnWriteArrayList<FileTreeNodeForTask> children;

    /**
     * QA对数量
     */
    private int qaCount;

    public FileTreeNodeForTask(FileTreeNode node) {
        this.name = node.getName();
        this.type = node.getType();
        this.fileId = node.getFileId();
        this.children = deepCopyChildren(node.getChildren());
        this.qaCount = 0;
    }

    private CopyOnWriteArrayList<FileTreeNodeForTask> deepCopyChildren(CopyOnWriteArrayList<FileTreeNode> originalChildren) {
        CopyOnWriteArrayList<FileTreeNodeForTask> copiedChildren = new CopyOnWriteArrayList<>();
        for (FileTreeNode originalChild : originalChildren) {
            // 对每个子节点进行深拷贝
            FileTreeNodeForTask copiedChild = new FileTreeNodeForTask(originalChild);
            copiedChildren.add(copiedChild);
        }
        return copiedChildren;
    }
}
