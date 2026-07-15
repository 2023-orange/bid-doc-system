package com.example.biddoc.folder.dto.resp;

import com.example.biddoc.document.dto.resp.TagRespDTO;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class FolderDetailRespDTO {

    private Long id;
    private Long parentId;
    private String name;
    private String fullPath;
    private String parentName;
    private String ancestorIds;
    private Integer level;
    private Integer sortNo;
    private Long ownerDeptId;
    private String ownerDeptName;
    private Long ownerUserId;
    private String ownerUserName;
    private Boolean inheritPermission;
    private Integer status;
    private String remark;
    private OffsetDateTime createdAt;
    private String createdBy;
    private String createdByName;
    private OffsetDateTime updatedAt;
    private String updatedBy;
    private String updatedByName;
    private Long documentCount;
    private Long totalDocumentCount;
    private Long folderSize;
    private Long totalSize;
    private Long childFolderCount;
    private Long totalChildFolderCount;
    private FolderPermissionRespDTO permissions;
    private Long viewCount;
    private OffsetDateTime lastAccessTime;
    private String lastAccessUserName;
    private Long downloadCount;
    private Boolean isFavorite;
    private List<RelatedProjectRespDTO> relatedProjects = new ArrayList<>();
    private List<TagRespDTO> tags = new ArrayList<>();

    @Data
    public static class RelatedProjectRespDTO {
        private Long projectId;
        private String projectName;
        private String projectNo;
    }
}
