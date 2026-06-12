package com.example.biddoc.project.service;

public interface ProjectPermissionService {
    void checkView(Long projectId);
    void checkManage(Long projectId);
    void checkManageOrOwner(Long projectId, Long checklistOwnerUserId);
}
