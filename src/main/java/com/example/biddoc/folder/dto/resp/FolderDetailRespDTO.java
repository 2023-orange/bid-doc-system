package com.example.biddoc.folder.dto.resp;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FolderDetailRespDTO {

    private Long id;
    private Long parentId;
    private String name;
    private String ancestorIds;
    private Integer level;
    private Integer sortNo;
    private Long ownerDeptId;
    private Long ownerUserId;
    private Boolean inheritPermission;
    private Integer status;
    private String remark;
    private OffsetDateTime createdAt;
    private String createdBy;
    private OffsetDateTime updatedAt;
    private String updatedBy;
}
