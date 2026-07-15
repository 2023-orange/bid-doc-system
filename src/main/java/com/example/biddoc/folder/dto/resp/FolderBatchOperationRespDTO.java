package com.example.biddoc.folder.dto.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FolderBatchOperationRespDTO {

    private Integer successCount;
    private Integer failedCount;
    private List<FailedItem> failedItems = new ArrayList<>();

    public static FolderBatchOperationRespDTO success(int successCount) {
        FolderBatchOperationRespDTO dto = new FolderBatchOperationRespDTO();
        dto.setSuccessCount(successCount);
        dto.setFailedCount(0);
        return dto;
    }

    @Data
    public static class FailedItem {
        private Long id;
        private String reason;
    }
}
