package com.example.biddoc.document.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.document.dto.resp.DocumentDetailRespDTO;
import com.example.biddoc.document.dto.resp.DocumentListItemRespDTO;
import com.example.biddoc.document.dto.resp.DocumentUploadRespDTO;
import com.example.biddoc.document.dto.resp.DocumentVersionRespDTO;
import com.example.biddoc.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文档管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "文档管理", description = "文档上传、下载、版本管理")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 上传新文档（首版本）
     */
    @PostMapping("/folders/{folderId}/documents")
    @Operation(summary = "上传新文档", description = "在指定文件夹下上传新文档（首版本）")
    public ApiResponse<DocumentUploadRespDTO> uploadDocument(
            @PathVariable Long folderId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "remark", required = false) String remark,
            @RequestParam(value = "changeLog", required = false) String changeLog) {

        DocumentUploadRespDTO result = documentService.uploadDocument(folderId, file, name, remark, changeLog);
        return ApiResponse.success(result);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/documents/{id}")
    @Operation(summary = "获取文档详情", description = "获取文档详情，包含当前版本信息")
    public ApiResponse<DocumentDetailRespDTO> getDocumentDetail(@PathVariable Long id) {
        DocumentDetailRespDTO result = documentService.getDocumentDetail(id);
        return ApiResponse.success(result);
    }

    /**
     * 获取文件夹下的文档列表
     */
    @GetMapping("/folders/{folderId}/documents")
    @Operation(summary = "获取文档列表", description = "获取指定文件夹下的文档列表（分页）")
    public ApiResponse<PageResponse<DocumentListItemRespDTO>> listDocuments(
            @PathVariable Long folderId,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "createdAt") String sort,
            @RequestParam(value = "order", required = false, defaultValue = "desc") String order) {

        PageResponse<DocumentListItemRespDTO> result = documentService.listDocuments(folderId, page, size, sort, order);
        return ApiResponse.success(result);
    }

    /**
     * 软删除文档
     */
    @DeleteMapping("/documents/{id}")
    @Operation(summary = "删除文档", description = "软删除文档及其所有版本")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ApiResponse.success(null);
    }

    /**
     * 上传新版本
     */
    @PostMapping("/documents/{id}/versions")
    @Operation(summary = "上传新版本", description = "为已有文档上传新版本")
    public ApiResponse<DocumentVersionRespDTO> uploadNewVersion(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "changeLog", required = false) String changeLog) {

        DocumentVersionRespDTO result = documentService.uploadNewVersion(id, file, changeLog);
        return ApiResponse.success(result);
    }

    /**
     * 获取文档的版本列表
     */
    @GetMapping("/documents/{id}/versions")
    @Operation(summary = "获取版本列表", description = "获取文档的所有版本列表（按版本号降序）")
    public ApiResponse<List<DocumentVersionRespDTO>> listVersions(@PathVariable Long id) {
        List<DocumentVersionRespDTO> result = documentService.listVersions(id);
        return ApiResponse.success(result);
    }

    /**
     * 下载文档（当前版本）
     */
    @GetMapping("/documents/{id}/download")
    @Operation(summary = "下载文档", description = "下载文档的当前版本")
    public void downloadDocument(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        DocumentService.DownloadResult result = documentService.downloadDocument(
                id,
                resolveClientIp(request),
                request.getHeader("User-Agent")
        );
        writeFileResponse(result, response, true);
    }

    /**
     * 下载指定版本
     */
    @GetMapping("/documents/{id}/versions/{versionNo}/download")
    @Operation(summary = "下载指定版本", description = "下载文档的指定版本")
    public void downloadVersion(@PathVariable Long id, @PathVariable Integer versionNo,
                                HttpServletRequest request, HttpServletResponse response) {
        DocumentService.DownloadResult result = documentService.downloadVersion(
                id,
                versionNo,
                resolveClientIp(request),
                request.getHeader("User-Agent")
        );
        writeFileResponse(result, response, true);
    }

    /**
     * 预览文档（当前版本）
     */
    @GetMapping("/documents/{id}/preview")
    @Operation(summary = "预览文档", description = "预览文档的当前版本，仅支持 PDF、图片和文本")
    public void previewDocument(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        DocumentService.DownloadResult result = documentService.previewDocument(
                id,
                resolveClientIp(request),
                request.getHeader("User-Agent")
        );
        writeFileResponse(result, response, false);
    }

    /**
     * 预览指定版本
     */
    @GetMapping("/documents/{id}/versions/{versionNo}/preview")
    @Operation(summary = "预览指定版本", description = "预览文档的指定版本，仅支持 PDF、图片和文本")
    public void previewVersion(@PathVariable Long id, @PathVariable Integer versionNo,
                               HttpServletRequest request, HttpServletResponse response) {
        DocumentService.DownloadResult result = documentService.previewVersion(
                id,
                versionNo,
                resolveClientIp(request),
                request.getHeader("User-Agent")
        );
        writeFileResponse(result, response, false);
    }

    /**
     * 写入文件响应
     */
    private void writeFileResponse(DocumentService.DownloadResult result, HttpServletResponse response, boolean attachment) {
        try {
            // 设置响应头
            response.setContentType(result.getMimeType() != null ? result.getMimeType() : "application/octet-stream");
            response.setContentLengthLong(result.getSize());
            response.setHeader("Cache-Control", "no-store");

            // 设置 Content-Disposition（RFC 5987 编码）
            String encodedFilename = URLEncoder.encode(result.getOriginalFilename(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setHeader("Content-Disposition",
                    (attachment ? "attachment" : "inline") + "; filename*=UTF-8''" + encodedFilename);

            // 写入流
            try (InputStream inputStream = result.getInputStream();
                 OutputStream outputStream = response.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }

        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 搜索文档
     */
    @GetMapping("/documents/search")
    @Operation(summary = "搜索文档", description = "按文档名称模糊搜索，支持文件夹范围过滤、权限过滤、分页")
    public ApiResponse<PageResponse<DocumentListItemRespDTO>> searchDocuments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "recursive", required = false, defaultValue = "true") Boolean recursive,
            @RequestParam(value = "mimeType", required = false) String mimeType,
            @RequestParam(value = "ownerUserId", required = false) Long ownerUserId,
            @RequestParam(value = "createdFrom", required = false) java.time.OffsetDateTime createdFrom,
            @RequestParam(value = "createdTo", required = false) java.time.OffsetDateTime createdTo,
            @RequestParam(value = "favoriteFolderOnly", required = false, defaultValue = "false") Boolean favoriteFolderOnly,
            @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
            @RequestParam(value = "sortBy", required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {

        com.example.biddoc.document.dto.req.DocumentSearchReqDTO searchReq =
                new com.example.biddoc.document.dto.req.DocumentSearchReqDTO();
        searchReq.setKeyword(keyword);
        searchReq.setFolderId(folderId);
        searchReq.setRecursive(recursive);
        searchReq.setMimeType(mimeType);
        searchReq.setOwnerUserId(ownerUserId);
        searchReq.setCreatedFrom(createdFrom);
        searchReq.setCreatedTo(createdTo);
        searchReq.setFavoriteFolderOnly(favoriteFolderOnly);
        searchReq.setTagIds(tagIds);
        searchReq.setSortBy(sortBy);
        searchReq.setSortOrder(sortOrder);
        searchReq.setPage(page);
        searchReq.setSize(size);

        PageResponse<DocumentListItemRespDTO> result = documentService.searchDocuments(searchReq);
        return ApiResponse.success(result);
    }

    /**
     * 获取搜索历史
     */
    @GetMapping("/documents/search/history")
    @Operation(summary = "获取搜索历史", description = "获取当前用户的搜索历史记录")
    public ApiResponse<List<com.example.biddoc.document.dto.resp.SearchHistoryRespDTO>> getSearchHistory(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        List<com.example.biddoc.document.dto.resp.SearchHistoryRespDTO> result =
                documentService.getSearchHistory(limit);
        return ApiResponse.success(result);
    }

    /**
     * 清除搜索历史
     */
    @DeleteMapping("/documents/search/history")
    @Operation(summary = "清除搜索历史", description = "清除当前用户的所有搜索历史记录")
    public ApiResponse<Void> clearSearchHistory() {
        documentService.clearSearchHistory();
        return ApiResponse.success(null);
    }

    /**
     * 获取热门搜索
     */
    @GetMapping("/documents/search/hot")
    @Operation(summary = "获取热门搜索", description = "获取热门搜索关键词排行")
    public ApiResponse<List<com.example.biddoc.document.dto.resp.HotSearchRespDTO>> getHotSearchKeywords(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(value = "days", required = false, defaultValue = "7") Integer days) {
        List<com.example.biddoc.document.dto.resp.HotSearchRespDTO> result =
                documentService.getHotSearchKeywords(limit, days);
        return ApiResponse.success(result);
    }

    /**
     * 获取存储空间统计
     */
    @GetMapping("/documents/stats/storage")
    @Operation(summary = "获取存储空间统计", description = "获取文档总数、存储空间等统计信息")
    public ApiResponse<com.example.biddoc.document.dto.resp.StorageStatsRespDTO> getStorageStats() {
        com.example.biddoc.document.dto.resp.StorageStatsRespDTO result = documentService.getStorageStats();
        return ApiResponse.success(result);
    }

    /**
     * 获取文档类型分布统计
     */
    @GetMapping("/documents/stats/types")
    @Operation(summary = "获取文档类型分布", description = "按MIME类型统计文档数量和大小")
    public ApiResponse<List<com.example.biddoc.document.dto.resp.DocumentTypeStatsRespDTO>> getDocumentTypeStats(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        List<com.example.biddoc.document.dto.resp.DocumentTypeStatsRespDTO> result =
                documentService.getDocumentTypeStats(limit);
        return ApiResponse.success(result);
    }

    /**
     * 获取热门文档排行
     */
    @GetMapping("/documents/stats/popular")
    @Operation(summary = "获取热门文档排行", description = "按下载次数统计热门文档")
    public ApiResponse<List<com.example.biddoc.document.dto.resp.PopularDocumentRespDTO>> getPopularDocuments(
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(value = "days", required = false, defaultValue = "30") Integer days) {
        List<com.example.biddoc.document.dto.resp.PopularDocumentRespDTO> result =
                documentService.getPopularDocuments(limit, days);
        return ApiResponse.success(result);
    }
}
