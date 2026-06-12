package com.example.biddoc.project.service;

import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.project.dto.req.ChecklistTemplateItemSaveReqDTO;
import com.example.biddoc.project.dto.req.ChecklistTemplateSaveReqDTO;
import com.example.biddoc.project.entity.ChecklistTemplateEntity;

public interface ChecklistTemplateService {
    Long create(ChecklistTemplateSaveReqDTO req);
    void update(Long id, ChecklistTemplateSaveReqDTO req);
    ChecklistTemplateEntity get(Long id);
    PageResponse<ChecklistTemplateEntity> list(String projectType, Integer page, Integer size);
    void delete(Long id);
    Long addItem(Long templateId, ChecklistTemplateItemSaveReqDTO req);
    void updateItem(Long templateId, Long itemId, ChecklistTemplateItemSaveReqDTO req);
    void deleteItem(Long templateId, Long itemId);
}
