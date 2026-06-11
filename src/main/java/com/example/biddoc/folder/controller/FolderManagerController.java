package com.example.biddoc.folder.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.folder.dto.req.FolderManagerSaveReqDTO;
import com.example.biddoc.folder.dto.resp.FolderManagerRespDTO;
import com.example.biddoc.folder.service.FolderManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
public class FolderManagerController {

    private final FolderManagerService folderManagerService;

    @GetMapping("/{folderId}/managers")
    public ApiResponse<List<FolderManagerRespDTO>> list(@PathVariable Long folderId) {
        return ApiResponse.success(folderManagerService.list(folderId));
    }

    @PostMapping("/{folderId}/managers")
    public ApiResponse<Void> add(@PathVariable Long folderId,
                                 @Valid @RequestBody FolderManagerSaveReqDTO req) {
        folderManagerService.add(folderId, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{folderId}/managers/{managerId}")
    public ApiResponse<Void> remove(@PathVariable Long folderId,
                                    @PathVariable Long managerId) {
        folderManagerService.remove(folderId, managerId);
        return ApiResponse.success();
    }
}
