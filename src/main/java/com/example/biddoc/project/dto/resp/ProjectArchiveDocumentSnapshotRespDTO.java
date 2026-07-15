package com.example.biddoc.project.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProjectArchiveDocumentSnapshotRespDTO {
    private Long id;
    private Long archiveRecordId;
    private Long projectId;
    private Long checklistItemId;
    private Long documentId;
    private Integer versionNo;
    private String documentName;
    private String documentStatus;
    private String versionStatus;
    private OffsetDateTime expireAt;
    private String storageType;
    private Long fileSize;
    private String snapshotJson;
}
