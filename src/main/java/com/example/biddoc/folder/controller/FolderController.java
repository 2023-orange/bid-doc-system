package com.example.biddoc.folder.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.folder.dto.req.FolderBatchDeleteReqDTO;
import com.example.biddoc.folder.dto.req.FolderCopyReqDTO;
import com.example.biddoc.folder.dto.req.FolderCreateReqDTO;
import com.example.biddoc.folder.dto.req.FolderMoveReqDTO;
import com.example.biddoc.folder.dto.req.FolderRenameReqDTO;
import com.example.biddoc.folder.dto.req.FolderUpdateReqDTO;
import com.example.biddoc.folder.dto.resp.FolderDetailRespDTO;
import com.example.biddoc.folder.dto.resp.FolderPermissionRespDTO;
import com.example.biddoc.folder.dto.resp.FolderTreeNodeRespDTO;
import com.example.biddoc.folder.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody FolderCreateReqDTO req) {
        Long id = folderService.create(req);
        return ApiResponse.success(Map.of("id", id));
    }

    @GetMapping("/{id}")
    public ApiResponse<FolderDetailRespDTO> get(@PathVariable Long id) {
        return ApiResponse.success(folderService.getById(id));
    }

    @PatchMapping("/{id}/name")
    public ApiResponse<Void> rename(@PathVariable Long id, @Valid @RequestBody FolderRenameReqDTO req) {
        folderService.rename(id, req);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody FolderUpdateReqDTO req) {
        folderService.update(id, req);
        return ApiResponse.success();
    }

    @GetMapping("/children")
    public ApiResponse<List<FolderTreeNodeRespDTO>> listChildren(@RequestParam(required = false) Long parentId) {
        return ApiResponse.success(folderService.listChildren(parentId));
    }

    @GetMapping("/tree/root")
    public ApiResponse<List<FolderTreeNodeRespDTO>> listRootTree() {
        return ApiResponse.success(folderService.listRootTree());
    }

    @GetMapping("/{id}/permissions/me")
    public ApiResponse<FolderPermissionRespDTO> getMyPermissions(@PathVariable Long id) {
        return ApiResponse.success(folderService.getMyPermissions(id));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<FolderTreeNodeRespDTO>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "parentId", required = false) Long parentId,
            @RequestParam(value = "recursive", required = false, defaultValue = "true") Boolean recursive,
            @RequestParam(value = "favoriteOnly", required = false, defaultValue = "false") Boolean favoriteOnly,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        return ApiResponse.success(folderService.search(keyword, parentId, recursive, favoriteOnly, page, size));
    }

    // ============================================================
    // Phase 4：move / copy / delete / batchDelete
    // ============================================================

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        folderService.delete(id);
        return ApiResponse.success();
    }

    @DeleteMapping("/batch")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody FolderBatchDeleteReqDTO req) {
        folderService.batchDelete(req);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/move")
    public ApiResponse<Void> move(@PathVariable Long id, @Valid @RequestBody FolderMoveReqDTO req) {
        folderService.move(id, req);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/copy")
    public ApiResponse<Map<String, Long>> copy(@PathVariable Long id, @Valid @RequestBody FolderCopyReqDTO req) {
        Long newRootId = folderService.copy(id, req);
        return ApiResponse.success(Map.of("id", newRootId));
    }
}
