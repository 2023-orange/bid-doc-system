package com.example.biddoc.workflow.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.workflow.dto.req.ApprovalHandleReqDTO;
import com.example.biddoc.workflow.dto.req.ApprovalSubmitReqDTO;
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

    @GetMapping("/documents/{id}/approval/history")
    public ApiResponse<List<ApprovalHistoryRespDTO>> history(@PathVariable Long id) {
        return ApiResponse.success(approvalService.history(id));
    }
}
