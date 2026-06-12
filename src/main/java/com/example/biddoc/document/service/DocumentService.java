package com.example.biddoc.document.service;

import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.document.dto.req.DocumentMetadataUpdateReqDTO;
import com.example.biddoc.document.dto.req.DocumentSearchReqDTO;
import com.example.biddoc.document.dto.resp.DocumentDetailRespDTO;
import com.example.biddoc.document.dto.resp.DocumentListItemRespDTO;
import com.example.biddoc.document.dto.resp.DocumentUploadRespDTO;
import com.example.biddoc.document.dto.resp.DocumentVersionRespDTO;
import com.example.biddoc.document.dto.resp.SearchHistoryRespDTO;
import com.example.biddoc.document.dto.resp.HotSearchRespDTO;
import com.example.biddoc.document.dto.resp.StorageStatsRespDTO;
import com.example.biddoc.document.dto.resp.DocumentTypeStatsRespDTO;
import com.example.biddoc.document.dto.resp.PopularDocumentRespDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 文档服务接口
 */
public interface DocumentService {

    /**
     * 上传新文档（首版本）
     *
     * @param folderId  文件夹 ID
     * @param file      上传文件
     * @param name      文档名称（可选，为空时使用原始文件名）
     * @param remark    备注
     * @param changeLog 版本说明
     * @return 上传响应
     */
    DocumentUploadRespDTO uploadDocument(Long folderId, MultipartFile file, String name,
                                         String remark, String changeLog);

    /**
     * 获取文档详情
     *
     * @param documentId 文档 ID
     * @return 文档详情
     */
    DocumentDetailRespDTO getDocumentDetail(Long documentId);

    void updateMetadata(Long documentId, DocumentMetadataUpdateReqDTO req);

    void markApproving(Long documentId);

    void markApprovalResult(Long documentId, boolean approved, String reason);

    void voidDocument(Long documentId, String reason);

    void restoreDocument(Long documentId);

    PageResponse<DocumentListItemRespDTO> listBindableDocuments(DocumentSearchReqDTO searchReq);

    /**
     * 获取文件夹下的文档列表（分页）
     *
     * @param folderId 文件夹 ID
     * @param page     页码
     * @param size     每页大小
     * @param sort     排序字段（createdAt/name/size）
     * @param order    排序方向（asc/desc）
     * @return 文档列表
     */
    PageResponse<DocumentListItemRespDTO> listDocuments(Long folderId, Integer page, Integer size,
                                                        String sort, String order);

    /**
     * 软删除文档
     *
     * @param documentId 文档 ID
     */
    void deleteDocument(Long documentId);

    /**
     * 上传新版本
     *
     * @param documentId 文档 ID
     * @param file       上传文件
     * @param changeLog  版本说明
     * @return 版本响应
     */
    DocumentVersionRespDTO uploadNewVersion(Long documentId, MultipartFile file, String changeLog);

    /**
     * 获取文档的版本列表
     *
     * @param documentId 文档 ID
     * @return 版本列表（按版本号降序）
     */
    List<DocumentVersionRespDTO> listVersions(Long documentId);

    /**
     * 下载文档（当前版本）
     *
     * @param documentId 文档 ID
     * @return 输入流和版本信息的包装对象
     */
    DownloadResult downloadDocument(Long documentId);

    /**
     * 下载文档（当前版本），并记录请求上下文到下载统计。
     */
    DownloadResult downloadDocument(Long documentId, String ipAddress, String userAgent);

    /**
     * 下载指定版本
     *
     * @param documentId 文档 ID
     * @param versionNo  版本号
     * @return 输入流和版本信息的包装对象
     */
    DownloadResult downloadVersion(Long documentId, Integer versionNo);

    /**
     * 下载指定版本，并记录请求上下文到下载统计。
     */
    DownloadResult downloadVersion(Long documentId, Integer versionNo, String ipAddress, String userAgent);

    /**
     * 预览文档（当前版本），仅支持浏览器可直接预览的 MIME 类型。
     */
    DownloadResult previewDocument(Long documentId, String ipAddress, String userAgent);

    /**
     * 预览指定版本，仅支持浏览器可直接预览的 MIME 类型。
     */
    DownloadResult previewVersion(Long documentId, Integer versionNo, String ipAddress, String userAgent);

    /**
     * 级联软删除指定 folder ID 集合下的所有文档及其版本
     * 由 FolderServiceImpl.doBatchDelete 在自身事务内调用
     *
     * @param folderIds 已确认被软删除的 folder ID 集合（可空，空时直接返回 0）
     * @return 受影响的 document 数量（用于审计 extraData）
     */
    int cascadeSoftDeleteByFolderIds(List<Long> folderIds);

    /**
     * 搜索文档
     * 支持按文档名称模糊搜索、文件夹范围过滤、权限过滤、分页
     *
     * @param searchReq 搜索请求参数
     * @return 文档列表（分页）
     */
    PageResponse<DocumentListItemRespDTO> searchDocuments(DocumentSearchReqDTO searchReq);

    /**
     * 获取当前用户的搜索历史
     *
     * @param limit 返回数量限制（默认10条）
     * @return 搜索历史列表
     */
    List<SearchHistoryRespDTO> getSearchHistory(Integer limit);

    /**
     * 清除当前用户的搜索历史
     */
    void clearSearchHistory();

    /**
     * 获取热门搜索关键词
     *
     * @param limit 返回数量限制（默认10条）
     * @param days  统计天数（默认7天）
     * @return 热门搜索列表
     */
    List<HotSearchRespDTO> getHotSearchKeywords(Integer limit, Integer days);

    /**
     * 获取存储空间统计
     *
     * @return 存储空间统计信息
     */
    StorageStatsRespDTO getStorageStats();

    /**
     * 获取文档类型分布统计
     *
     * @param limit 返回数量限制（默认10条）
     * @return 文档类型分布列表
     */
    List<DocumentTypeStatsRespDTO> getDocumentTypeStats(Integer limit);

    /**
     * 获取热门文档排行
     *
     * @param limit 返回数量限制（默认10条）
     * @param days  统计天数（默认30天）
     * @return 热门文档列表
     */
    List<PopularDocumentRespDTO> getPopularDocuments(Integer limit, Integer days);

    /**
     * 下载结果包装类
     */
    class DownloadResult {
        private final InputStream inputStream;
        private final String originalFilename;
        private final String mimeType;
        private final Long size;
        private final Integer versionNo;

        public DownloadResult(InputStream inputStream, String originalFilename, String mimeType,
                              Long size, Integer versionNo) {
            this.inputStream = inputStream;
            this.originalFilename = originalFilename;
            this.mimeType = mimeType;
            this.size = size;
            this.versionNo = versionNo;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public String getMimeType() {
            return mimeType;
        }

        public Long getSize() {
            return size;
        }

        public Integer getVersionNo() {
            return versionNo;
        }
    }
}
