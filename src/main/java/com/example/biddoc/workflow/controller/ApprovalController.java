package com.example.biddoc.workflow.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.workflow.dto.req.ApprovalAddSignReqDTO;
import com.example.biddoc.workflow.dto.req.ApprovalHandleReqDTO;
import com.example.biddoc.workflow.dto.req.ApprovalSubmitReqDTO;
import com.example.biddoc.workflow.dto.req.ApprovalTerminateReqDTO;
import com.example.biddoc.workflow.dto.req.ApprovalTransferReqDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalHistoryRespDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalTaskRespDTO;
import com.example.biddoc.workflow.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping("/documents/{id}/approval/submit")
    public ApiResponse<Map<String, Long>> submit(@PathVariable Long id,
                                                 @RequestBody(required = false) ApprovalSubmitReqDTO req) {
        Long instanceId = approvalService.submit(id, req != null ? req.getComment() : null);
        return ApiResponse.success(Map.of("instanceId", instanceId));
    }

    @PostMapping("/documents/{id}/versions/{versionNo}/approval/submit")
    public ApiResponse<Map<String, Long>> submitVersion(@PathVariable Long id, @PathVariable Integer versionNo,
                                                        @RequestBody(required = false) ApprovalSubmitReqDTO req) {
        Long instanceId = approvalService.submitVersion(id, versionNo, req != null ? req.getComment() : null);
        return ApiResponse.success(Map.of("instanceId", instanceId));
    }

    @PostMapping("/projects/{projectId}/checklist/items/{itemId}/approval/submit")
    public ApiResponse<Map<String, Long>> submitChecklistItem(@PathVariable Long projectId, @PathVariable Long itemId,
                                                              @RequestBody(required = false) ApprovalSubmitReqDTO req) {
        Long instanceId = approvalService.submitChecklistItem(projectId, itemId, req != null ? req.getComment() : null);
        return ApiResponse.success(Map.of("instanceId", instanceId));
    }

    @GetMapping("/approvals/tasks")
    public ApiResponse<List<ApprovalTaskRespDTO>> listMyTasks(
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.success(approvalService.listMyTasks(status));
    }

    @PostMapping("/approvals/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id,
                                     @RequestBody(required = false) ApprovalHandleReqDTO req) {
        approvalService.approve(id, req != null ? req.getComment() : null);
        return ApiResponse.success();
    }

    @PostMapping("/approvals/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id,
                                    @RequestBody(required = false) ApprovalHandleReqDTO req) {
        approvalService.reject(id, req != null ? req.getComment() : null);
        return ApiResponse.success();
    }

    @PostMapping("/approvals/{id}/withdraw")
    public ApiResponse<Void> withdraw(@PathVariable Long id,
                                      @RequestBody(required = false) ApprovalHandleReqDTO req) {
        approvalService.withdraw(id, req != null ? req.getComment() : null);
        return ApiResponse.success();
    }

    @PostMapping("/approvals/tasks/{taskId}/transfer")
    public ApiResponse<Void> transfer(@PathVariable Long taskId,
                                      @RequestBody(required = false) ApprovalTransferReqDTO req) {
        if (req == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "转交参数不能为空");
        }
        approvalService.transfer(taskId, req.getTargetUserId(), req.getComment());
        return ApiResponse.success();
    }

    @PostMapping("/approvals/tasks/{taskId}/add-sign")
    public ApiResponse<Void> addSign(@PathVariable Long taskId,
                                     @RequestBody(required = false) ApprovalAddSignReqDTO req) {
        if (req == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "加签参数不能为空");
        }
        approvalService.addSign(taskId, req.getAssigneeUserId(), req.getComment());
        return ApiResponse.success();
    }

    @PostMapping("/approvals/{id}/terminate")
    public ApiResponse<Void> terminate(@PathVariable Long id,
                                       @RequestBody(required = false) ApprovalTerminateReqDTO req) {
        if (req == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "终止参数不能为空");
        }
        approvalService.terminate(id, req.getReason());
        return ApiResponse.success();
    }

    @GetMapping("/documents/{id}/approval/history")
    public ApiResponse<List<ApprovalHistoryRespDTO>> history(@PathVariable Long id) {
        return ApiResponse.success(approvalService.history(id));
    }

    @GetMapping("/projects/{projectId}/approval/history")
    public ApiResponse<List<ApprovalHistoryRespDTO>> projectHistory(@PathVariable Long projectId) {
        return ApiResponse.success(approvalService.projectHistory(projectId));
    }
}
