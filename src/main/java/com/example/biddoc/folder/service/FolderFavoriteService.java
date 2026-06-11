package com.example.biddoc.folder.service;

import com.example.biddoc.folder.dto.resp.FolderFavoriteRespDTO;

import java.util.List;

public interface FolderFavoriteService {

    void favorite(Long folderId);

    void unfavorite(Long folderId);

    List<FolderFavoriteRespDTO> listFavorites();
}
