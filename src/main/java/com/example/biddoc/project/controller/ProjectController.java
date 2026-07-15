package com.example.biddoc.project.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.project.dto.req.ProjectCreateReqDTO;
import com.example.biddoc.project.dto.req.ProjectMemberSaveReqDTO;
import com.example.biddoc.project.dto.req.ProjectStageUpdateReqDTO;
import com.example.biddoc.project.dto.req.ProjectStatusUpdateReqDTO;
import com.example.biddoc.project.dto.req.ProjectUpdateReqDTO;
import com.example.biddoc.project.dto.resp.ProjectArchiveDetailRespDTO;
import com.example.biddoc.project.dto.resp.ProjectRespDTO;
import com.example.biddoc.project.dto.resp.ProjectWorkbenchRespDTO;
import com.example.biddoc.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody ProjectCreateReqDTO req) {
        return ApiResponse.success(Map.of("id", projectService.create(req)));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody ProjectUpdateReqDTO req) {
        projectService.update(id, req);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectRespDTO> get(@PathVariable Long id) {
        return ApiResponse.success(projectService.get(id));
    }

    @GetMapping("/{id}/workbench")
    public ApiResponse<ProjectWorkbenchRespDTO> getWorkbench(@PathVariable Long id) {
        return ApiResponse.success(projectService.getWorkbench(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProjectRespDTO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectNo,
            @RequestParam(required = false) Long ownerDeptId,
            @RequestParam(required = false) String projectType,
            @RequestParam(required = false) String projectStage,
            @RequestParam(required = false) String projectStatus,
            @RequestParam(required = false) Long ownerUserId,
            @RequestParam(required = false) OffsetDateTime deadlineFrom,
            @RequestParam(required = false) OffsetDateTime deadlineTo,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(projectService.list(keyword, projectNo, ownerDeptId, projectType,
                projectStage, projectStatus, ownerUserId, deadlineFrom, deadlineTo, page, size));
    }

    @PostMapping("/{id}/members")
    public ApiResponse<Void> addMembers(@PathVariable Long id, @Valid @RequestBody ProjectMemberSaveReqDTO req) {
        projectService.addMembers(id, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        projectService.removeMember(id, userId);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/stage")
    public ApiResponse<Void> changeStage(@PathVariable Long id, @Valid @RequestBody ProjectStageUpdateReqDTO req) {
        projectService.changeStage(id, req.getProjectStage());
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody ProjectStatusUpdateReqDTO req) {
        projectService.changeStatus(id, req.getProjectStatus());
        return ApiResponse.success();
    }

    @GetMapping("/{id}/archive")
    public ApiResponse<ProjectArchiveDetailRespDTO> getArchiveDetail(@PathVariable Long id) {
        return ApiResponse.success(projectService.getArchiveDetail(id));
    }
}
