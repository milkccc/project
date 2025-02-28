package com.xzzn.pollux.model.pojo;

import com.xzzn.pollux.entity.es.FileContent;
import com.xzzn.pollux.entity.es.QADocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author xzzn
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentContentPair implements Serializable {

    private String fileId;

    private FileContentInfo fileContent;

    private List<QADocument> qaDocumentList;

    @Data
    public static class FileContentInfo implements Serializable{
        private String id;
        private String content;
        private String fileName;

        public FileContentInfo(FileContent fileContent, String fileName) {
            this.id = fileContent.getId();
            this.content = fileContent.getContent();
            this.fileName = fileName;
        }
    }

}
