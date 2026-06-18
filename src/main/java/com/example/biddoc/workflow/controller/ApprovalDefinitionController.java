package com.example.biddoc.workflow.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.workflow.dto.req.ApprovalDefinitionSaveReqDTO;
import com.example.biddoc.workflow.dto.req.ApprovalNodeSaveReqDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalDefinitionRespDTO;
import com.example.biddoc.workflow.service.ApprovalDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflow/definitions")
@RequiredArgsConstructor
public class ApprovalDefinitionController {

    private final ApprovalDefinitionService approvalDefinitionService;

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@RequestBody ApprovalDefinitionSaveReqDTO req) {
        return ApiResponse.success(Map.of("id", approvalDefinitionService.create(req)));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody ApprovalDefinitionSaveReqDTO req) {
        approvalDefinitionService.update(id, req);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<ApprovalDefinitionRespDTO> get(@PathVariable Long id) {
        return ApiResponse.success(approvalDefinitionService.get(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ApprovalDefinitionRespDTO>> list(
            @RequestParam(required = false) String scenario,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(approvalDefinitionService.list(scenario, bizType, enabled, page, size));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enable(@PathVariable Long id) {
        approvalDefinitionService.enable(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        approvalDefinitionService.disable(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/nodes")
    public ApiResponse<Map<String, Long>> addNode(@PathVariable Long id, @RequestBody ApprovalNodeSaveReqDTO req) {
        return ApiResponse.success(Map.of("id", approvalDefinitionService.addNode(id, req)));
    }

    @PutMapping("/{id}/nodes/{nodeId}")
    public ApiResponse<Void> updateNode(@PathVariable Long id, @PathVariable Long nodeId,
                                        @RequestBody ApprovalNodeSaveReqDTO req) {
        approvalDefinitionService.updateNode(id, nodeId, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}/nodes/{nodeId}")
    public ApiResponse<Void> deleteNode(@PathVariable Long id, @PathVariable Long nodeId) {
        approvalDefinitionService.deleteNode(id, nodeId);
        return ApiResponse.success();
    }
}
