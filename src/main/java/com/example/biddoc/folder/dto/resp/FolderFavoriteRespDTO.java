package com.example.biddoc.folder.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FolderFavoriteRespDTO {

    private Long favoriteId;
    private Long folderId;
    private Long parentId;
    private String folderName;
    private Integer level;
    private Integer sortNo;
    private OffsetDateTime createdAt;
    private String createdBy;
}
