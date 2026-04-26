package com.example.biddoc.folder.service;

import com.example.biddoc.folder.dto.req.FolderCreateReqDTO;
import com.example.biddoc.folder.dto.req.FolderRenameReqDTO;
import com.example.biddoc.folder.dto.req.FolderUpdateReqDTO;
import com.example.biddoc.folder.dto.resp.FolderDetailRespDTO;
import com.example.biddoc.folder.dto.resp.FolderTreeNodeRespDTO;

import java.util.List;

public interface FolderService {

    Long create(FolderCreateReqDTO req);

    FolderDetailRespDTO getById(Long id);

    void rename(Long id, FolderRenameReqDTO req);

    void update(Long id, FolderUpdateReqDTO req);

    List<FolderTreeNodeRespDTO> listChildren(Long parentId);

    List<FolderTreeNodeRespDTO> listRootTree();
}
