package com.example.biddoc.folder.service;

import com.example.biddoc.document.dto.resp.TagRespDTO;
import com.example.biddoc.folder.dto.req.FolderAccessRecordReqDTO;
import com.example.biddoc.folder.dto.req.FolderTagBindReqDTO;
import com.example.biddoc.folder.dto.resp.FolderActionResultRespDTO;
import com.example.biddoc.folder.dto.resp.FolderDetailRespDTO;
import com.example.biddoc.folder.dto.resp.FolderPermissionRespDTO;
import com.example.biddoc.folder.dto.resp.FolderShortcutRespDTO;
import com.example.biddoc.folder.dto.resp.FolderStatsRespDTO;
import com.example.biddoc.folder.dto.resp.FolderTreeNodeRespDTO;
import com.example.biddoc.folder.entity.FolderEntity;

import java.util.List;

public interface FolderInsightService {

    FolderStatsRespDTO getStats();

    FolderDetailRespDTO getDetail(Long id);

    List<FolderShortcutRespDTO> listRecent(Integer limit);

    List<FolderShortcutRespDTO> listFavorites(Integer limit);

    List<FolderShortcutRespDTO> listManaged(Integer limit);

    List<FolderShortcutRespDTO> listOwned(Integer limit);

    void recordAccess(Long id, FolderAccessRecordReqDTO req);

    List<TagRespDTO> listTags(Long id);

    void bindTags(Long id, FolderTagBindReqDTO req);

    FolderPermissionRespDTO buildPermissions(FolderEntity folder);

    FolderActionResultRespDTO toActionResult(Long folderId);

    void fillTreeNodeExtras(List<FolderTreeNodeRespDTO> nodes);
}
