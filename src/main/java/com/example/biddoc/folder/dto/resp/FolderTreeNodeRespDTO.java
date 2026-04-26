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
    private OffsetDateTime createdAt;
}
