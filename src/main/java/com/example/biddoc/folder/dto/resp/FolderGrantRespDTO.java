package com.example.biddoc.folder.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FolderGrantRespDTO {

    private Long grantId;
    private Long folderId;
    private String subjectType;
    private String subjectId;
    private String permissionCode;
    private String grantScope;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    private OffsetDateTime createdAt;
    private String createdBy;
}
