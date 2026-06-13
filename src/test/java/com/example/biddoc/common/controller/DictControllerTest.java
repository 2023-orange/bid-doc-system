package com.example.biddoc.common.controller;

import com.example.biddoc.common.dto.resp.DictItemRespDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictControllerTest {

    private final DictController controller = new DictController();

    @Test
    void listAllReturnsFrontendCoreBusinessEnums() {
        Map<String, List<DictItemRespDTO>> data = controller.listAll().getData();

        assertEquals(List.of("NORMAL", "PAUSED", "ARCHIVED", "CANCELLED", "DELETED"),
                codes(data.get("projectStatus")));
        assertEquals(List.of("COLLECTING", "REVIEWING", "COMPILING", "WAITING_SUBMIT", "SUBMITTED",
                "ARCHIVED", "CANCELLED"), codes(data.get("projectStage")));
        assertEquals(List.of("OWNER", "MATERIAL_OWNER", "MEMBER"), codes(data.get("projectMemberRole")));
        assertEquals(List.of("PENDING_COLLECT", "PENDING_REVIEW", "NEED_SUPPLEMENT", "COMPLETE", "ARCHIVED"),
                codes(data.get("checklistItemStatus")));
        assertEquals(List.of("INCOMPLETE", "READY_SUBMIT", "APPROVING", "APPROVED", "REJECTED", "VOIDED", "DELETED"),
                codes(data.get("documentStatus")));
        assertEquals(List.of("PUBLIC", "INTERNAL", "SENSITIVE", "SECRET"), codes(data.get("sensitiveLevel")));
        assertEquals(List.of("PENDING", "APPROVED", "REJECTED"), codes(data.get("approvalStatus")));
        assertEquals(List.of("DOCUMENT", "DOCUMENT_VERSION", "CHECKLIST_ITEM"), codes(data.get("approvalBizType")));
    }

    @Test
    void listByTypeReturnsOnlyRequestedType() {
        List<DictItemRespDTO> data = controller.listByType("approvalStatus").getData();

        assertEquals(List.of("PENDING", "APPROVED", "REJECTED"), codes(data));
        assertTrue(data.stream().allMatch(item -> "approvalStatus".equals(item.getType())));
    }

    private static List<String> codes(List<DictItemRespDTO> items) {
        return items.stream().map(DictItemRespDTO::getCode).collect(Collectors.toList());
    }
}
