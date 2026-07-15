package com.example.biddoc.document.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.document.dto.req.DocumentUseGrantCreateReqDTO;
import com.example.biddoc.document.dto.resp.DocumentUseGrantRespDTO;
import com.example.biddoc.document.service.DocumentUseGrantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents/{documentId}/use-grants")
@RequiredArgsConstructor
@Tag(name = "资料使用授权", description = "资料授权、撤销和查询")
public class DocumentUseGrantController {

    private final DocumentUseGrantService documentUseGrantService;

    @PostMapping
    @Operation(summary = "新增资料使用授权", description = "为指定资料版本新增使用授权")
    public ApiResponse<Map<String, Long>> createGrant(@PathVariable Long documentId,
                                                      @RequestBody DocumentUseGrantCreateReqDTO req) {
        Long grantId = documentUseGrantService.createGrant(documentId, req);
        return ApiResponse.success(Map.of("grantId", grantId));
    }

    @DeleteMapping("/{grantId}")
    @Operation(summary = "撤销资料使用授权", description = "撤销指定资料授权")
    public ApiResponse<Void> revokeGrant(@PathVariable Long documentId, @PathVariable Long grantId) {
        documentUseGrantService.revokeGrant(documentId, grantId);
        return ApiResponse.success();
    }

    @GetMapping
    @Operation(summary = "查询资料使用授权", description = "查询指定资料的有效授权记录")
    public ApiResponse<List<DocumentUseGrantRespDTO>> listGrants(@PathVariable Long documentId) {
        return ApiResponse.success(documentUseGrantService.listGrants(documentId));
    }
}
