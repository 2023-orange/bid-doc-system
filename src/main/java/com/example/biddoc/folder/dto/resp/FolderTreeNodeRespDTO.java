package com.example.biddoc.folder.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FolderTreeNodeRespDTO {

    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sortNo;
    private Boolean hasChildren;
    private Long documentCount;
    private Long totalSize;
    private OffsetDateTime lastAccessTime;
    private OffsetDateTime createdAt;
}
