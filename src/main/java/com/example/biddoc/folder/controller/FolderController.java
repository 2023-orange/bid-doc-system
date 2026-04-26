package com.example.biddoc.folder.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.folder.dto.req.FolderCreateReqDTO;
import com.example.biddoc.folder.dto.req.FolderRenameReqDTO;
import com.example.biddoc.folder.dto.req.FolderUpdateReqDTO;
import com.example.biddoc.folder.dto.resp.FolderDetailRespDTO;
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
}
