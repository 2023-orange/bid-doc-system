package com.example.biddoc.common.controller;

import com.example.biddoc.common.dto.resp.DictItemRespDTO;
import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.project.constant.ChecklistItemStatusEnum;
import com.example.biddoc.project.constant.ProjectMemberRoleEnum;
import com.example.biddoc.project.constant.ProjectStageEnum;
import com.example.biddoc.project.constant.ProjectStatusEnum;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/dicts")
public class DictController {

    @GetMapping
    public ApiResponse<Map<String, List<DictItemRespDTO>>> listAll() {
        Map<String, List<DictItemRespDTO>> dicts = new LinkedHashMap<>();
        dicts.put("projectStatus", fromProjectStatus());
        dicts.put("projectStage", fromProjectStage());
        dicts.put("projectMemberRole", fromProjectMemberRole());
        dicts.put("checklistItemStatus", fromChecklistItemStatus());
        dicts.put("documentStatus", documentStatuses());
        dicts.put("sensitiveLevel", sensitiveLevels());
        dicts.put("approvalStatus", approvalStatuses());
        dicts.put("approvalBizType", approvalBizTypes());
        return ApiResponse.success(dicts);
    }

    @GetMapping("/{type}")
    public ApiResponse<List<DictItemRespDTO>> listByType(@PathVariable String type) {
        return ApiResponse.success(listAll().getData().getOrDefault(type, List.of()));
    }

    private List<DictItemRespDTO> fromProjectStatus() {
        AtomicInteger sort = new AtomicInteger(1);
        return Arrays.stream(ProjectStatusEnum.values())
                .map(item -> item("projectStatus", item.getCode(), item.getName(), null, sort.getAndIncrement()))
                .collect(Collectors.toList());
    }

    private List<DictItemRespDTO> fromProjectStage() {
        AtomicInteger sort = new AtomicInteger(1);
        return Arrays.stream(ProjectStageEnum.values())
                .map(item -> item("projectStage", item.getCode(), item.getName(), null, sort.getAndIncrement()))
                .collect(Collectors.toList());
    }

    private List<DictItemRespDTO> fromProjectMemberRole() {
        AtomicInteger sort = new AtomicInteger(1);
        return Arrays.stream(ProjectMemberRoleEnum.values())
                .map(item -> item("projectMemberRole", item.getCode(), item.getName(), null, sort.getAndIncrement()))
                .collect(Collectors.toList());
    }

    private List<DictItemRespDTO> fromChecklistItemStatus() {
        AtomicInteger sort = new AtomicInteger(1);
        return Arrays.stream(ChecklistItemStatusEnum.values())
                .map(item -> item("checklistItemStatus", item.getCode(), item.getName(), null, sort.getAndIncrement()))
                .collect(Collectors.toList());
    }

    private List<DictItemRespDTO> documentStatuses() {
        // 文档生命周期状态集中来自 DocumentServiceImpl 与 ProjectChecklistServiceImpl 中已经使用的业务字符串。
        return List.of(
                item("documentStatus", "INCOMPLETE", "Incomplete", "Uploaded document waiting for metadata", 1),
                item("documentStatus", "READY_SUBMIT", "Ready to submit", "Metadata completed and ready for approval", 2),
                item("documentStatus", "APPROVING", "Approving", "Document approval is in progress", 3),
                item("documentStatus", "APPROVED", "Approved", "Approved document can be bound to checklist items", 4),
                item("documentStatus", "REJECTED", "Rejected", "Rejected document needs supplement or correction", 5),
                item("documentStatus", "VOIDED", "Voided", "Document has been voided by business operation", 6),
                item("documentStatus", "DELETED", "Deleted", "Deleted state retained by checklist binding validation", 7)
        );
    }

    private List<DictItemRespDTO> sensitiveLevels() {
        // 敏感等级来自 DocumentServiceImpl 的敏感访问策略：PUBLIC/INTERNAL/SENSITIVE/SECRET。
        return List.of(
                item("sensitiveLevel", "PUBLIC", "Public", "No additional sensitive access restriction", 1),
                item("sensitiveLevel", "INTERNAL", "Internal", "Internal document with normal folder permission", 2),
                item("sensitiveLevel", "SENSITIVE", "Sensitive", "Preview allowed, download restricted for non-privileged users", 3),
                item("sensitiveLevel", "SECRET", "Secret", "Preview and download restricted for non-privileged users", 4)
        );
    }

    private List<DictItemRespDTO> approvalStatuses() {
        // 审批状态来自轻量审批与文档版本审批当前使用的 PENDING/APPROVED/REJECTED。
        return List.of(
                item("approvalStatus", "PENDING", "Pending", null, 1),
                item("approvalStatus", "APPROVED", "Approved", null, 2),
                item("approvalStatus", "REJECTED", "Rejected", null, 3)
        );
    }

    private List<DictItemRespDTO> approvalBizTypes() {
        // 审批业务类型来自 ApprovalServiceImpl 写入 wf_approval_instance.biz_type 的现有值。
        return List.of(
                item("approvalBizType", "DOCUMENT", "Document approval", null, 1),
                item("approvalBizType", "DOCUMENT_VERSION", "Document version approval", null, 2),
                item("approvalBizType", "CHECKLIST_ITEM", "Checklist item approval", null, 3)
        );
    }

    private static DictItemRespDTO item(String type, String code, String name, String description, Integer sort) {
        return new DictItemRespDTO(type, code, name, description, sort);
    }
}
