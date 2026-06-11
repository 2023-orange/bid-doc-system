package com.example.biddoc.folder.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.folder.dto.req.FolderGrantSaveReqDTO;
import com.example.biddoc.folder.dto.resp.FolderGrantRespDTO;
import com.example.biddoc.folder.service.FolderGrantService;
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
public class FolderGrantController {

    private final FolderGrantService folderGrantService;

    @GetMapping("/{folderId}/grants")
    public ApiResponse<List<FolderGrantRespDTO>> list(@PathVariable Long folderId) {
        return ApiResponse.success(folderGrantService.list(folderId));
    }

    @PostMapping("/{folderId}/grants")
    public ApiResponse<Void> add(@PathVariable Long folderId,
                                 @Valid @RequestBody FolderGrantSaveReqDTO req) {
        folderGrantService.add(folderId, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{folderId}/grants/{grantId}")
    public ApiResponse<Void> remove(@PathVariable Long folderId,
                                    @PathVariable Long grantId) {
        folderGrantService.remove(folderId, grantId);
        return ApiResponse.success();
    }
}
