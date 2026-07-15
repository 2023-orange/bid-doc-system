package com.example.biddoc.folder.service;

import com.example.biddoc.folder.dto.req.FolderBatchDeleteReqDTO;
import com.example.biddoc.folder.dto.req.FolderCopyReqDTO;
import com.example.biddoc.folder.dto.req.FolderCreateReqDTO;
import com.example.biddoc.folder.dto.req.FolderMoveReqDTO;
import com.example.biddoc.folder.dto.req.FolderRenameReqDTO;
import com.example.biddoc.folder.dto.req.FolderUpdateReqDTO;
import com.example.biddoc.folder.dto.resp.FolderActionResultRespDTO;
import com.example.biddoc.folder.dto.resp.FolderBatchOperationRespDTO;
import com.example.biddoc.folder.dto.resp.FolderDetailRespDTO;
import com.example.biddoc.folder.dto.resp.FolderPermissionRespDTO;
import com.example.biddoc.folder.dto.resp.FolderTreeNodeRespDTO;
import com.example.biddoc.common.result.PageResponse;

import java.util.List;

public interface FolderService {

    Long create(FolderCreateReqDTO req);

    FolderDetailRespDTO getById(Long id);

    void rename(Long id, FolderRenameReqDTO req);

    void update(Long id, FolderUpdateReqDTO req);

    List<FolderTreeNodeRespDTO> listChildren(Long parentId);

    List<FolderTreeNodeRespDTO> listRootTree();

    FolderPermissionRespDTO getMyPermissions(Long id);

    PageResponse<FolderTreeNodeRespDTO> search(String keyword, Long parentId, Boolean recursive,
                                               Boolean favoriteOnly, Integer page, Integer size);

    /**
     * 删除单个文件夹（含全部后代）。审计 op = DELETE。
     */
    void delete(Long id);

    /**
     * 批量删除文件夹（自动父子去重，含每个保留根的全部后代）。审计 op = BATCH_DELETE。
     */
    FolderBatchOperationRespDTO batchDelete(FolderBatchDeleteReqDTO req);

    /**
     * 将文件夹（含全部后代）移动到目标父目录下。审计 op = MOVE。
     */
    FolderActionResultRespDTO move(Long id, FolderMoveReqDTO req);

    /**
     * 将文件夹整子树复制到目标父目录下，不复制 grant/manager/favorite。
     * 返回新根节点 id。审计 op = COPY。
     */
    FolderActionResultRespDTO copy(Long id, FolderCopyReqDTO req);
}
