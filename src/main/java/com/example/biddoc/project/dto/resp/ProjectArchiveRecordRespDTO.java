package com.example.biddoc.project.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProjectArchiveRecordRespDTO {
    private Long id;
    private Long projectId;
    private String archiveNo;
    private String archiveStatus;
    private OffsetDateTime archivedAt;
    private Long archivedBy;
    private String archiveReason;
    private Integer checklistTotal;
    private Integer checklistComplete;
    private Integer documentTotal;
    private String snapshotHash;
    private String remark;
}
