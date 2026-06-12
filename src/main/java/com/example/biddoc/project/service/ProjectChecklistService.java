package com.example.biddoc.project.service;

import com.example.biddoc.project.entity.ProjectChecklistItemEntity;

import java.util.List;

public interface ProjectChecklistService {
    void generateFromTemplate(Long projectId, Long templateId);
    List<ProjectChecklistItemEntity> listItems(Long projectId);
    void updateOwner(Long projectId, Long itemId, Long ownerUserId, java.time.OffsetDateTime deadline);
    void bindDocument(Long projectId, Long itemId, Long documentId, Integer versionNo);
    void unbindDocument(Long projectId, Long itemId, Long documentId);
    void recalculateItemStatus(Long itemId);
}
