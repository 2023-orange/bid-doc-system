package com.example.biddoc.folder.dto.resp;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FolderStatsRespDTO {

    private Long totalFolders;
    private Long totalDocuments;
    private Long totalSize;
    private Long activeFolders;
    private Long favoriteFolders;
    private Long managedFolders;
    private Long ownedFolders;
    private Long sharedWithMeFolders;
    private Long recentAccessCount;
    private Long storageQuota;
    private BigDecimal storageUsageRate;
}
