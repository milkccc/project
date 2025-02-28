package com.xzzn.pollux.model.vo.response.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAUnallocatedFileIdResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Chunk> chunkList;

    private Integer totalUnallocatedCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chunk implements Serializable{
        private String fileId;
        private Integer UnallocatedCount;
        private Integer totalCount;
    }


}
