package com.example.biddoc.folder.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FolderManagerRespDTO {

    private Long managerId;
    private Long folderId;
    private Long userId;
    private String manageScope;
    private OffsetDateTime createdAt;
    private String createdBy;
}
