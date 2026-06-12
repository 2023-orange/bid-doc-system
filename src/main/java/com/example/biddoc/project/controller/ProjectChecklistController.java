package com.example.biddoc.project.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.project.dto.req.ChecklistOwnerUpdateReqDTO;
import com.example.biddoc.project.entity.ProjectChecklistItemEntity;
import com.example.biddoc.project.service.ProjectChecklistService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProjectChecklistController {

    private final ProjectChecklistService projectChecklistService;

    @PostMapping("/api/v1/projects/{projectId}/checklist/generate")
    public ApiResponse<Void> generate(@PathVariable Long projectId, @RequestParam Long templateId) {
        projectChecklistService.generateFromTemplate(projectId, templateId);
        return ApiResponse.success();
    }

    @GetMapping("/api/v1/projects/{projectId}/checklist")
    public ApiResponse<List<ProjectChecklistItemEntity>> list(@PathVariable Long projectId) {
        return ApiResponse.success(projectChecklistService.listItems(projectId));
    }

    @PatchMapping("/api/v1/projects/{projectId}/checklist/items/{itemId}/owner")
    public ApiResponse<Void> updateOwner(@PathVariable Long projectId, @PathVariable Long itemId,
                                         @RequestBody ChecklistOwnerUpdateReqDTO req) {
        projectChecklistService.updateOwner(projectId, itemId, req.getOwnerUserId(), req.getDeadline());
        return ApiResponse.success();
    }

    @PostMapping("/api/v1/projects/{projectId}/checklist/items/{itemId}/documents")
    public ApiResponse<Void> bind(@PathVariable Long projectId, @PathVariable Long itemId,
                                  @RequestBody BindDocumentReq req) {
        projectChecklistService.bindDocument(projectId, itemId, req.getDocumentId(), req.getVersionNo());
        return ApiResponse.success();
    }

    @DeleteMapping("/api/v1/projects/{projectId}/checklist/items/{itemId}/documents/{documentId}")
    public ApiResponse<Void> unbind(@PathVariable Long projectId, @PathVariable Long itemId,
                                    @PathVariable Long documentId) {
        projectChecklistService.unbindDocument(projectId, itemId, documentId);
        return ApiResponse.success();
    }

    @Data
    public static class BindDocumentReq {
        private Long documentId;
        private Integer versionNo;
    }
}
