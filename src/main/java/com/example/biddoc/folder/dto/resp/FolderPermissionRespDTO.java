package com.example.biddoc.folder.dto.resp;

import lombok.Data;

@Data
public class FolderPermissionRespDTO {

    private Boolean canView;
    private Boolean canCreateChild;
    private Boolean canRename;
    private Boolean canEdit;
    private Boolean canDelete;
    private Boolean canMove;
    private Boolean canCopy;
    private Boolean canFavorite;
    private Boolean isOwner;
    private Boolean isManager;
    private Boolean isSuperAdmin;
}
