package com.example.biddoc.folder.service;

import com.example.biddoc.folder.dto.req.FolderManagerSaveReqDTO;
import com.example.biddoc.folder.dto.resp.FolderManagerRespDTO;

import java.util.List;

public interface FolderManagerService {

    List<FolderManagerRespDTO> list(Long folderId);

    void add(Long folderId, FolderManagerSaveReqDTO req);

    void remove(Long folderId, Long managerId);
}
