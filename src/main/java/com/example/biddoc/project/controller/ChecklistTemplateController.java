package com.example.biddoc.project.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.project.dto.req.ChecklistTemplateItemSaveReqDTO;
import com.example.biddoc.project.dto.req.ChecklistTemplateSaveReqDTO;
import com.example.biddoc.project.entity.ChecklistTemplateEntity;
import com.example.biddoc.project.service.ChecklistTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/checklist-templates")
@RequiredArgsConstructor
public class ChecklistTemplateController {

    private final ChecklistTemplateService checklistTemplateService;

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@RequestBody ChecklistTemplateSaveReqDTO req) {
        return ApiResponse.success(Map.of("id", checklistTemplateService.create(req)));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody ChecklistTemplateSaveReqDTO req) {
        checklistTemplateService.update(id, req);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<ChecklistTemplateEntity> get(@PathVariable Long id) {
        return ApiResponse.success(checklistTemplateService.get(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ChecklistTemplateEntity>> list(
            @RequestParam(required = false) String projectType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(checklistTemplateService.list(projectType, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        checklistTemplateService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/items")
    public ApiResponse<Map<String, Long>> addItem(@PathVariable Long id,
                                                  @RequestBody ChecklistTemplateItemSaveReqDTO req) {
        return ApiResponse.success(Map.of("id", checklistTemplateService.addItem(id, req)));
    }

    @PutMapping("/{id}/items/{itemId}")
    public ApiResponse<Void> updateItem(@PathVariable Long id, @PathVariable Long itemId,
                                        @RequestBody ChecklistTemplateItemSaveReqDTO req) {
        checklistTemplateService.updateItem(id, itemId, req);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        checklistTemplateService.deleteItem(id, itemId);
        return ApiResponse.success();
    }
}
