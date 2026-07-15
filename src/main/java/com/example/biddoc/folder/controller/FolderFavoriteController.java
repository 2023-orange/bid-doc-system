package com.example.biddoc.folder.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.folder.dto.resp.FolderShortcutRespDTO;
import com.example.biddoc.folder.service.FolderFavoriteService;
import com.example.biddoc.folder.service.FolderInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
public class FolderFavoriteController {

    private final FolderFavoriteService folderFavoriteService;
    private final FolderInsightService folderInsightService;

    @GetMapping("/favorites")
    public ApiResponse<List<FolderShortcutRespDTO>> listFavorites(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        return ApiResponse.success(folderInsightService.listFavorites(limit));
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
