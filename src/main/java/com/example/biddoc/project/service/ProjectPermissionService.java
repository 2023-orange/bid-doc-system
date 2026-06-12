package com.example.biddoc.project.service;

public interface ProjectPermissionService {
    void checkView(Long projectId);
    void checkManage(Long projectId);
    void checkManageOrOwner(Long projectId, Long checklistOwnerUserId);
    void checkChecklistMaintain(Long projectId, Long checklistOwnerUserId);
    boolean isProjectOwner(Long projectId, Long userId);
    boolean isProjectMaterialOwner(Long projectId, Long userId);
    boolean isProjectMember(Long projectId, Long userId);
}
