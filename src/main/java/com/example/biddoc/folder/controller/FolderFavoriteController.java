package com.example.biddoc.folder.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.folder.dto.resp.FolderFavoriteRespDTO;
import com.example.biddoc.folder.service.FolderFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
public class FolderFavoriteController {

    private final FolderFavoriteService folderFavoriteService;

    @GetMapping("/favorites")
    public ApiResponse<List<FolderFavoriteRespDTO>> listFavorites() {
        return ApiResponse.success(folderFavoriteService.listFavorites());
    }

    @PostMapping("/{folderId}/favorite")
    public ApiResponse<Void> favorite(@PathVariable Long folderId) {
        folderFavoriteService.favorite(folderId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{folderId}/favorite")
    public ApiResponse<Void> unfavorite(@PathVariable Long folderId) {
        folderFavoriteService.unfavorite(folderId);
        return ApiResponse.success();
    }
}
