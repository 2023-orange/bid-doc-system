package com.example.biddoc.project.dto.resp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectArchiveDetailRespDTO {
    private ProjectArchiveRecordRespDTO record;
    private List<ProjectArchiveChecklistSnapshotRespDTO> checklistSnapshots = new ArrayList<>();
    private List<ProjectArchiveDocumentSnapshotRespDTO> documentSnapshots = new ArrayList<>();
}
