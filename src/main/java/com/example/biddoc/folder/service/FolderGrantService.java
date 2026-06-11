package com.example.biddoc.folder.service;

import com.example.biddoc.folder.dto.req.FolderGrantSaveReqDTO;
import com.example.biddoc.folder.dto.resp.FolderGrantRespDTO;

import java.util.List;

public interface FolderGrantService {

    List<FolderGrantRespDTO> list(Long folderId);

    void add(Long folderId, FolderGrantSaveReqDTO req);

    void remove(Long folderId, Long grantId);
}
