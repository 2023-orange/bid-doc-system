package com.example.biddoc.project.dto.resp;

import lombok.Data;

@Data
public class ProjectArchiveChecklistSnapshotRespDTO {
    private Long id;
    private Long archiveRecordId;
    private Long projectId;
    private Long checklistItemId;
    private Long templateItemId;
    private String itemName;
    private Boolean requiredFlag;
    private String itemStatus;
    private Integer boundDocumentCount;
    private String snapshotJson;
}
