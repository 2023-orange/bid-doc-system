package com.example.biddoc.folder.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FolderShortcutRespDTO {

    private Long id;
    private String name;
    private Long parentId;
    private String fullPath;
    private Integer level;
    private Integer sortNo;
    private Long documentCount;
    private OffsetDateTime lastAccessTime;

    // 兼容原 favorites 响应字段，避免前端旧调用立即失效。
    private Long favoriteId;
    private Long folderId;
    private String folderName;
    private OffsetDateTime createdAt;
    private String createdBy;
}
