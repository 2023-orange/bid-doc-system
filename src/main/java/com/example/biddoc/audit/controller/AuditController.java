package com.example.biddoc.audit.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.example.biddoc.audit.dto.req.AuditQueryReqDTO;
import com.example.biddoc.audit.dto.resp.AuditLogRespDTO;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.common.result.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    @SaCheckRole("SUPER_ADMIN")
    public ApiResponse<PageResponse<AuditLogRespDTO>> queryLogs(@ModelAttribute AuditQueryReqDTO req) {
        return ApiResponse.success(auditService.queryLogs(req));
    }

    @GetMapping("/logs/{id}")
    @SaCheckRole("SUPER_ADMIN")
    public ApiResponse<AuditLogRespDTO> queryLogDetail(@PathVariable Long id) {
        return ApiResponse.success(auditService.queryLogDetail(id));
    }

    @GetMapping("/timeline")
    @SaCheckRole("SUPER_ADMIN")
    public ApiResponse<List<AuditLogRespDTO>> queryTimeline(@ModelAttribute AuditQueryReqDTO req) {
        return ApiResponse.success(auditService.queryTimeline(req));
    }
}
