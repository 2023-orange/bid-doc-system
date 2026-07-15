package com.example.biddoc.workflow.service.impl;

import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.workflow.dto.req.ApprovalDefinitionSaveReqDTO;
import com.example.biddoc.workflow.dto.req.ApprovalConditionSaveReqDTO;
import com.example.biddoc.workflow.dto.req.ApprovalNodeSaveReqDTO;
import com.example.biddoc.workflow.entity.ApprovalConditionEntity;
import com.example.biddoc.workflow.entity.ApprovalDefinitionEntity;
import com.example.biddoc.workflow.entity.ApprovalNodeEntity;
import com.example.biddoc.workflow.mapper.ApprovalConditionMapper;
import com.example.biddoc.workflow.mapper.ApprovalDefinitionMapper;
import com.example.biddoc.workflow.mapper.ApprovalNodeMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalDefinitionServiceImplTest {

    private final ApprovalDefinitionMapper approvalDefinitionMapper = mock(ApprovalDefinitionMapper.class);
    private final ApprovalNodeMapper approvalNodeMapper = mock(ApprovalNodeMapper.class);
    private final ApprovalConditionMapper approvalConditionMapper = mock(ApprovalConditionMapper.class);
    private final AuditService auditService = mock(AuditService.class);
    private final ApprovalDefinitionServiceImpl service = new ApprovalDefinitionServiceImpl(
            approvalDefinitionMapper,
            approvalNodeMapper,
            approvalConditionMapper,
            auditService
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createRequiresSuperAdmin() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 10L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(definitionReq()));

        assertEquals(ErrorCode.ROLE_NOT_MATCH, ex.getErrorCode());
    }

    @Test
    void createInsertsDisabledVersionOneDefinition() {
        superAdmin();

        service.create(definitionReq());

        ArgumentCaptor<ApprovalDefinitionEntity> captor = ArgumentCaptor.forClass(ApprovalDefinitionEntity.class);
        verify(approvalDefinitionMapper).insert(captor.capture());
        ApprovalDefinitionEntity entity = captor.getValue();
        assertEquals("资料审批流程", entity.getName());
        assertEquals("DOCUMENT_APPROVAL", entity.getScenario());
        assertEquals("DOCUMENT", entity.getBizModule());
        assertEquals("DOCUMENT", entity.getBizType());
        assertEquals(1, entity.getVersion());
        assertFalse(entity.getEnabled());
        assertFalse(entity.getDeleted());
        verify(auditService).record(any());
    }

    @Test
    void enableRequiresAtLeastOneApprovalNode() {
        superAdmin();
        ApprovalDefinitionEntity definition = definition(100L, false);
        when(approvalDefinitionMapper.selectById(100L)).thenReturn(definition);
        when(approvalNodeMapper.selectCount(any())).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.enable(100L));

        assertEquals(ErrorCode.APPROVAL_FLOW_INVALID, ex.getErrorCode());
    }

    @Test
    void enableUpdatesDefinitionWhenApprovalNodeExists() {
        superAdmin();
        ApprovalDefinitionEntity definition = definition(100L, false);
        when(approvalDefinitionMapper.selectById(100L)).thenReturn(definition);
        when(approvalNodeMapper.selectCount(any())).thenReturn(1L);
        when(approvalNodeMapper.selectOne(any())).thenReturn(nodeReqAsEntity());

        service.enable(100L);

        assertTrue(definition.getEnabled());
        verify(approvalDefinitionMapper).updateById(definition);
        verify(auditService).record(any());
    }

    @Test
    void enabledDefinitionCannotAddNode() {
        superAdmin();
        when(approvalDefinitionMapper.selectById(100L)).thenReturn(definition(100L, true));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.addNode(100L, nodeReq()));

        assertEquals(ErrorCode.APPROVAL_FLOW_INVALID, ex.getErrorCode());
    }

    @Test
    void enabledDefinitionCannotUpdate() {
        superAdmin();
        when(approvalDefinitionMapper.selectById(100L)).thenReturn(definition(100L, true));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(100L, definitionReq()));

        assertEquals(ErrorCode.APPROVAL_FLOW_INVALID, ex.getErrorCode());
    }

    @Test
    void addNodeUsesDefaultsAndSavesConditions() {
        superAdmin();
        when(approvalDefinitionMapper.selectById(100L)).thenReturn(definition(100L, false));
        ApprovalNodeSaveReqDTO req = nodeReq();
        ApprovalConditionSaveReqDTO condition = new ApprovalConditionSaveReqDTO();
        condition.setConditionCode("TECH_ONLY");
        condition.setFieldName("bizType");
        condition.setCompareValue("DOCUMENT");
        condition.setTargetNodeCode("FINAL_REVIEW");
        req.setConditions(List.of(condition));

        service.addNode(100L, req);

        ArgumentCaptor<ApprovalNodeEntity> nodeCaptor = ArgumentCaptor.forClass(ApprovalNodeEntity.class);
        verify(approvalNodeMapper).insert(nodeCaptor.capture());
        ApprovalNodeEntity node = nodeCaptor.getValue();
        assertEquals("APPROVAL", node.getNodeType());
        assertEquals("ANY", node.getApproveMode());
        assertEquals(0, node.getSortOrder());
        assertFalse(node.getDeleted());

        ArgumentCaptor<ApprovalConditionEntity> conditionCaptor = ArgumentCaptor.forClass(ApprovalConditionEntity.class);
        verify(approvalConditionMapper).insert(conditionCaptor.capture());
        ApprovalConditionEntity savedCondition = conditionCaptor.getValue();
        assertEquals("TECH_ONLY", savedCondition.getConditionCode());
        assertEquals("EQ", savedCondition.getOperator());
        assertEquals("FINAL_REVIEW", savedCondition.getTargetNodeCode());
        assertFalse(savedCondition.getDeleted());
    }

    @Test
    void findEnabledDefinitionPrefersDeptAndBusinessCategoryThenVersion() {
        ApprovalDefinitionEntity global = matchDefinition(1L, null, null, 5);
        ApprovalDefinitionEntity deptOnly = matchDefinition(2L, 10L, null, 1);
        ApprovalDefinitionEntity exactOlder = matchDefinition(3L, 10L, "TECHNICAL", 1);
        ApprovalDefinitionEntity exactNewer = matchDefinition(4L, 10L, "TECHNICAL", 2);
        when(approvalDefinitionMapper.selectList(any())).thenReturn(List.of(global, exactOlder, deptOnly, exactNewer));

        ApprovalDefinitionEntity matched = service.findEnabledDefinition(
                "DOCUMENT_APPROVAL", "DOCUMENT", "DOCUMENT", 10L, "TECHNICAL");

        assertEquals(4L, matched.getId());
    }

    private void superAdmin() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 1L));
    }

    private ApprovalDefinitionSaveReqDTO definitionReq() {
        ApprovalDefinitionSaveReqDTO req = new ApprovalDefinitionSaveReqDTO();
        req.setName("资料审批流程");
        req.setScenario("DOCUMENT_APPROVAL");
        req.setBizModule("DOCUMENT");
        req.setBizType("DOCUMENT");
        req.setDeptId(10L);
        req.setBusinessCategory("TECHNICAL");
        return req;
    }

    private ApprovalNodeSaveReqDTO nodeReq() {
        ApprovalNodeSaveReqDTO req = new ApprovalNodeSaveReqDTO();
        req.setNodeCode("DEPT_REVIEW");
        req.setNodeName("部门审批");
        req.setAssigneeType("USER");
        req.setAssigneeValue("2");
        return req;
    }

    private ApprovalDefinitionEntity definition(Long id, boolean enabled) {
        ApprovalDefinitionEntity entity = new ApprovalDefinitionEntity();
        entity.setId(id);
        entity.setName("资料审批流程");
        entity.setEnabled(enabled);
        entity.setDeleted(false);
        entity.setVersion(1);
        return entity;
    }

    private ApprovalDefinitionEntity matchDefinition(Long id, Long deptId, String businessCategory, Integer version) {
        ApprovalDefinitionEntity entity = definition(id, true);
        entity.setScenario("DOCUMENT_APPROVAL");
        entity.setBizModule("DOCUMENT");
        entity.setBizType("DOCUMENT");
        entity.setDeptId(deptId);
        entity.setBusinessCategory(businessCategory);
        entity.setVersion(version);
        return entity;
    }

    private ApprovalNodeEntity nodeReqAsEntity() {
        ApprovalNodeEntity node = new ApprovalNodeEntity();
        node.setId(200L);
        node.setNodeType("APPROVAL");
        node.setDeleted(false);
        return node;
    }
}
