package com.example.biddoc.project.service;

import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.project.dto.req.ProjectCreateReqDTO;
import com.example.biddoc.project.dto.req.ProjectMemberSaveReqDTO;
import com.example.biddoc.project.dto.req.ProjectUpdateReqDTO;
import com.example.biddoc.project.dto.resp.ProjectArchiveDetailRespDTO;
import com.example.biddoc.project.dto.resp.ProjectRespDTO;
import com.example.biddoc.project.dto.resp.ProjectWorkbenchRespDTO;

import java.time.OffsetDateTime;

public interface ProjectService {

    Long create(ProjectCreateReqDTO req);

    void update(Long id, ProjectUpdateReqDTO req);

    ProjectRespDTO get(Long id);

    ProjectWorkbenchRespDTO getWorkbench(Long id);

    PageResponse<ProjectRespDTO> list(String keyword, String projectNo, Long ownerDeptId, String projectType,
                                      String projectStage, String projectStatus, Long ownerUserId,
                                      OffsetDateTime deadlineFrom, OffsetDateTime deadlineTo,
                                      Integer page, Integer size);

    void addMembers(Long projectId, ProjectMemberSaveReqDTO req);

    void removeMember(Long projectId, Long userId);

    void changeStage(Long projectId, String projectStage);

    void changeStatus(Long projectId, String projectStatus);

    ProjectArchiveDetailRespDTO getArchiveDetail(Long projectId);
}
