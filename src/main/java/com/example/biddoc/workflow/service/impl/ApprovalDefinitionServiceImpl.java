package com.example.biddoc.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.audit.constant.AuditModuleCodeEnum;
import com.example.biddoc.audit.constant.AuditOperationTypeEnum;
import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.workflow.dto.req.ApprovalConditionSaveReqDTO;
import com.example.biddoc.workflow.dto.req.ApprovalDefinitionSaveReqDTO;
import com.example.biddoc.workflow.dto.req.ApprovalNodeSaveReqDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalConditionRespDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalDefinitionRespDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalNodeRespDTO;
import com.example.biddoc.workflow.entity.ApprovalConditionEntity;
import com.example.biddoc.workflow.entity.ApprovalDefinitionEntity;
import com.example.biddoc.workflow.entity.ApprovalNodeEntity;
import com.example.biddoc.workflow.mapper.ApprovalConditionMapper;
import com.example.biddoc.workflow.mapper.ApprovalDefinitionMapper;
import com.example.biddoc.workflow.mapper.ApprovalNodeMapper;
import com.example.biddoc.workflow.service.ApprovalDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApprovalDefinitionServiceImpl implements ApprovalDefinitionService {

    private static final String MODULE_WORKFLOW = "WORKFLOW";

    private final ApprovalDefinitionMapper approvalDefinitionMapper;
    private final ApprovalNodeMapper approvalNodeMapper;
    private final ApprovalConditionMapper approvalConditionMapper;
    private final AuditService auditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ApprovalDefinitionSaveReqDTO req) {
        requireSuperAdmin();
        validateDefinition(req);

        ApprovalDefinitionEntity entity = new ApprovalDefinitionEntity();
        entity.setId(IdWorker.getId());
        entity.setName(req.getName());
        entity.setScenario(req.getScenario());
        entity.setBizModule(req.getBizModule());
        entity.setBizType(req.getBizType());
        entity.setDeptId(req.getDeptId());
        entity.setBusinessCategory(req.getBusinessCategory());
        entity.setVersion(1);
        entity.setEnabled(Boolean.FALSE);
        entity.setDeleted(Boolean.FALSE);
        approvalDefinitionMapper.insert(entity);

        recordDefinitionAudit(entity.getId(), AuditOperationTypeEnum.WORKFLOW_DEFINITION_CREATE.getCode(),
                Map.of("name", entity.getName(), "scenario", entity.getScenario(), "bizType", entity.getBizType()));
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ApprovalDefinitionSaveReqDTO req) {
        requireSuperAdmin();
        validateDefinition(req);
        ApprovalDefinitionEntity entity = requireDefinition(id);
        ensureDefinitionEditable(entity);

        entity.setName(req.getName());
        entity.setScenario(req.getScenario());
        entity.setBizModule(req.getBizModule());
        entity.setBizType(req.getBizType());
        entity.setDeptId(req.getDeptId());
        entity.setBusinessCategory(req.getBusinessCategory());
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1);
        approvalDefinitionMapper.updateById(entity);

        recordDefinitionAudit(id, AuditOperationTypeEnum.WORKFLOW_DEFINITION_UPDATE.getCode(),
                Map.of("version", entity.getVersion(), "scenario", entity.getScenario(), "bizType", entity.getBizType()));
    }

    @Override
    public ApprovalDefinitionRespDTO get(Long id) {
        requireSuperAdmin();
        return toDefinitionResp(requireDefinition(id), true);
    }

    @Override
    public PageResponse<ApprovalDefinitionRespDTO> list(String scenario, String bizType, Boolean enabled,
                                                       Integer page, Integer size) {
        requireSuperAdmin();
        Page<ApprovalDefinitionEntity> result = approvalDefinitionMapper.selectPage(
                new Page<>(page == null || page < 1 ? 1 : page, size == null || size < 1 ? 20 : size),
                Wrappers.<ApprovalDefinitionEntity>lambdaQuery()
                        .eq(StringUtils.hasText(scenario), ApprovalDefinitionEntity::getScenario, scenario)
                        .eq(StringUtils.hasText(bizType), ApprovalDefinitionEntity::getBizType, bizType)
                        .eq(enabled != null, ApprovalDefinitionEntity::getEnabled, enabled)
                        .eq(ApprovalDefinitionEntity::getDeleted, false)
                        .orderByDesc(ApprovalDefinitionEntity::getUpdatedAt));
        Page<ApprovalDefinitionRespDTO> mapped = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        mapped.setRecords(result.getRecords().stream().map(entity -> toDefinitionResp(entity, false)).toList());
        return PageResponse.of(mapped);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long id) {
        requireSuperAdmin();
        ApprovalDefinitionEntity entity = requireDefinition(id);
        long nodeCount = approvalNodeMapper.selectCount(Wrappers.<ApprovalNodeEntity>lambdaQuery()
                .eq(ApprovalNodeEntity::getDefinitionId, id)
                .eq(ApprovalNodeEntity::getNodeType, "APPROVAL")
                .eq(ApprovalNodeEntity::getDeleted, false));
        if (nodeCount <= 0) {
            throw new BusinessException(ErrorCode.APPROVAL_FLOW_INVALID, "启用流程前至少需要配置一个审批节点");
        }
        ApprovalNodeEntity firstNode = approvalNodeMapper.selectOne(Wrappers.<ApprovalNodeEntity>lambdaQuery()
                .eq(ApprovalNodeEntity::getDefinitionId, id)
                .eq(ApprovalNodeEntity::getDeleted, false)
                .orderByAsc(ApprovalNodeEntity::getSortOrder)
                .orderByAsc(ApprovalNodeEntity::getCreatedAt)
                .last("limit 1"));
        if (firstNode == null || "END".equals(firstNode.getNodeType())) {
            throw new BusinessException(ErrorCode.APPROVAL_FLOW_INVALID, "流程首节点不能是结束节点");
        }
        entity.setEnabled(Boolean.TRUE);
        approvalDefinitionMapper.updateById(entity);
        recordDefinitionAudit(id, AuditOperationTypeEnum.WORKFLOW_DEFINITION_ENABLE.getCode(), Map.of("enabled", true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        requireSuperAdmin();
        ApprovalDefinitionEntity entity = requireDefinition(id);
        entity.setEnabled(Boolean.FALSE);
        approvalDefinitionMapper.updateById(entity);
        recordDefinitionAudit(id, AuditOperationTypeEnum.WORKFLOW_DEFINITION_DISABLE.getCode(), Map.of("enabled", false));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addNode(Long definitionId, ApprovalNodeSaveReqDTO req) {
        requireSuperAdmin();
        ApprovalDefinitionEntity definition = requireDefinition(definitionId);
        ensureDefinitionEditable(definition);
        validateNode(req);

        ApprovalNodeEntity node = new ApprovalNodeEntity();
        node.setId(IdWorker.getId());
        node.setDefinitionId(definitionId);
        applyNode(node, req);
        node.setDeleted(Boolean.FALSE);
        approvalNodeMapper.insert(node);
        saveConditions(definitionId, node.getId(), req);

        recordDefinitionAudit(definitionId, AuditOperationTypeEnum.WORKFLOW_DEFINITION_UPDATE.getCode(),
                Map.of("addNodeId", String.valueOf(node.getId()), "nodeCode", node.getNodeCode()));
        return node.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNode(Long definitionId, Long nodeId, ApprovalNodeSaveReqDTO req) {
        requireSuperAdmin();
        ApprovalDefinitionEntity definition = requireDefinition(definitionId);
        ensureDefinitionEditable(definition);
        validateNode(req);
        ApprovalNodeEntity node = requireNode(definitionId, nodeId);
        applyNode(node, req);
        approvalNodeMapper.updateById(node);
        deleteConditions(definitionId, nodeId);
        saveConditions(definitionId, nodeId, req);
        recordDefinitionAudit(definitionId, AuditOperationTypeEnum.WORKFLOW_DEFINITION_UPDATE.getCode(),
                Map.of("updateNodeId", String.valueOf(nodeId), "nodeCode", node.getNodeCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNode(Long definitionId, Long nodeId) {
        requireSuperAdmin();
        ApprovalDefinitionEntity definition = requireDefinition(definitionId);
        ensureDefinitionEditable(definition);
        ApprovalNodeEntity node = requireNode(definitionId, nodeId);
        node.setDeleted(Boolean.TRUE);
        approvalNodeMapper.updateById(node);
        deleteConditions(definitionId, nodeId);
        recordDefinitionAudit(definitionId, AuditOperationTypeEnum.WORKFLOW_DEFINITION_UPDATE.getCode(),
                Map.of("deleteNodeId", String.valueOf(nodeId)));
    }

    @Override
    public ApprovalNodeRespDTO getFirstNode(Long definitionId) {
        ApprovalNodeEntity node = approvalNodeMapper.selectOne(Wrappers.<ApprovalNodeEntity>lambdaQuery()
                .eq(ApprovalNodeEntity::getDefinitionId, definitionId)
                .eq(ApprovalNodeEntity::getDeleted, false)
                .orderByAsc(ApprovalNodeEntity::getSortOrder)
                .orderByAsc(ApprovalNodeEntity::getCreatedAt)
                .last("limit 1"));
        if (node == null) {
            throw new BusinessException(ErrorCode.APPROVAL_FLOW_INVALID, "流程未配置首节点");
        }
        return toNodeResp(node);
    }

    public ApprovalDefinitionEntity findEnabledDefinition(String scenario, String bizModule, String bizType,
                                                         Long deptId, String businessCategory) {
        List<ApprovalDefinitionEntity> candidates = approvalDefinitionMapper.selectList(
                Wrappers.<ApprovalDefinitionEntity>lambdaQuery()
                        .eq(ApprovalDefinitionEntity::getScenario, scenario)
                        .eq(ApprovalDefinitionEntity::getBizModule, bizModule)
                        .eq(ApprovalDefinitionEntity::getBizType, bizType)
                        .eq(ApprovalDefinitionEntity::getEnabled, true)
                        .eq(ApprovalDefinitionEntity::getDeleted, false));
        return candidates.stream()
                .filter(def -> def.getDeptId() == null || def.getDeptId().equals(deptId))
                .filter(def -> !StringUtils.hasText(def.getBusinessCategory())
                        || def.getBusinessCategory().equals(businessCategory))
                // 流程匹配优先级在 Java 侧固定下来，避免不同数据库对 NULL 排序不一致导致命中错误流程。
                .sorted((left, right) -> {
                    int scoreCompare = Integer.compare(matchScore(right), matchScore(left));
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    return Integer.compare(right.getVersion() == null ? 0 : right.getVersion(),
                            left.getVersion() == null ? 0 : left.getVersion());
                })
                .findFirst()
                .orElse(null);
    }

    private int matchScore(ApprovalDefinitionEntity definition) {
        int score = 0;
        if (definition.getDeptId() != null) {
            score += 2;
        }
        if (StringUtils.hasText(definition.getBusinessCategory())) {
            score += 1;
        }
        return score;
    }

    public ApprovalNodeEntity findNodeByCode(Long definitionId, String nodeCode) {
        if (!StringUtils.hasText(nodeCode)) {
            return null;
        }
        return approvalNodeMapper.selectOne(Wrappers.<ApprovalNodeEntity>lambdaQuery()
                .eq(ApprovalNodeEntity::getDefinitionId, definitionId)
                .eq(ApprovalNodeEntity::getNodeCode, nodeCode)
                .eq(ApprovalNodeEntity::getDeleted, false)
                .last("limit 1"));
    }

    private void applyNode(ApprovalNodeEntity node, ApprovalNodeSaveReqDTO req) {
        node.setNodeCode(req.getNodeCode());
        node.setNodeName(req.getNodeName());
        node.setNodeType(StringUtils.hasText(req.getNodeType()) ? req.getNodeType() : "APPROVAL");
        node.setApproveMode(StringUtils.hasText(req.getApproveMode()) ? req.getApproveMode() : "ANY");
        node.setAssigneeType(req.getAssigneeType());
        node.setAssigneeValue(req.getAssigneeValue());
        node.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        node.setNextNodeCode(req.getNextNodeCode());
        node.setRejectToNodeCode(req.getRejectToNodeCode());
    }

    private ApprovalDefinitionEntity requireDefinition(Long id) {
        ApprovalDefinitionEntity entity = approvalDefinitionMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw new BusinessException(ErrorCode.APPROVAL_DEFINITION_NOT_FOUND);
        }
        return entity;
    }

    private ApprovalNodeEntity requireNode(Long definitionId, Long nodeId) {
        ApprovalNodeEntity node = approvalNodeMapper.selectById(nodeId);
        if (node == null || Boolean.TRUE.equals(node.getDeleted()) || !definitionId.equals(node.getDefinitionId())) {
            throw new BusinessException(ErrorCode.APPROVAL_NODE_NOT_FOUND);
        }
        return node;
    }

    private void ensureDefinitionEditable(ApprovalDefinitionEntity entity) {
        // 启用中的流程可能已有在途实例引用；一期不做完整节点快照，先禁止热修改以保护审批路径稳定。
        if (Boolean.TRUE.equals(entity.getEnabled())) {
            throw new BusinessException(ErrorCode.APPROVAL_FLOW_INVALID, "请先停用流程再修改配置");
        }
    }

    private void validateDefinition(ApprovalDefinitionSaveReqDTO req) {
        if (req == null || !StringUtils.hasText(req.getName()) || !StringUtils.hasText(req.getScenario())
                || !StringUtils.hasText(req.getBizModule()) || !StringUtils.hasText(req.getBizType())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "流程名称、场景、业务模块和业务类型不能为空");
        }
    }

    private void validateNode(ApprovalNodeSaveReqDTO req) {
        if (req == null || !StringUtils.hasText(req.getNodeCode()) || !StringUtils.hasText(req.getNodeName())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "节点编码和节点名称不能为空");
        }
        if (req.getConditions() != null) {
            for (ApprovalConditionSaveReqDTO condition : req.getConditions()) {
                if (condition == null || !StringUtils.hasText(condition.getConditionCode())
                        || !StringUtils.hasText(condition.getFieldName())
                        || !StringUtils.hasText(condition.getTargetNodeCode())) {
                    throw new BusinessException(ErrorCode.PARAM_INVALID, "条件编码、字段名和目标节点不能为空");
                }
            }
        }
    }

    private void saveConditions(Long definitionId, Long nodeId, ApprovalNodeSaveReqDTO req) {
        if (req.getConditions() == null || req.getConditions().isEmpty()) {
            return;
        }
        for (ApprovalConditionSaveReqDTO item : req.getConditions()) {
            ApprovalConditionEntity condition = new ApprovalConditionEntity();
            condition.setId(IdWorker.getId());
            condition.setDefinitionId(definitionId);
            condition.setNodeId(nodeId);
            condition.setConditionCode(item.getConditionCode());
            condition.setFieldName(item.getFieldName());
            condition.setOperator(StringUtils.hasText(item.getOperator()) ? item.getOperator() : "EQ");
            condition.setCompareValue(item.getCompareValue());
            condition.setTargetNodeCode(item.getTargetNodeCode());
            condition.setSortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder());
            condition.setDeleted(Boolean.FALSE);
            approvalConditionMapper.insert(condition);
        }
    }

    private void deleteConditions(Long definitionId, Long nodeId) {
        ApprovalConditionEntity update = new ApprovalConditionEntity();
        update.setDeleted(Boolean.TRUE);
        approvalConditionMapper.update(update, Wrappers.<ApprovalConditionEntity>lambdaUpdate()
                .eq(ApprovalConditionEntity::getDefinitionId, definitionId)
                .eq(ApprovalConditionEntity::getNodeId, nodeId)
                .eq(ApprovalConditionEntity::getDeleted, false));
    }

    private void requireSuperAdmin() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_MATCH);
        }
    }

    private ApprovalDefinitionRespDTO toDefinitionResp(ApprovalDefinitionEntity entity, boolean includeNodes) {
        ApprovalDefinitionRespDTO dto = new ApprovalDefinitionRespDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setScenario(entity.getScenario());
        dto.setBizModule(entity.getBizModule());
        dto.setBizType(entity.getBizType());
        dto.setDeptId(entity.getDeptId());
        dto.setBusinessCategory(entity.getBusinessCategory());
        dto.setVersion(entity.getVersion());
        dto.setEnabled(entity.getEnabled());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        if (includeNodes) {
            dto.setNodes(approvalNodeMapper.selectList(Wrappers.<ApprovalNodeEntity>lambdaQuery()
                    .eq(ApprovalNodeEntity::getDefinitionId, entity.getId())
                    .eq(ApprovalNodeEntity::getDeleted, false)
                    .orderByAsc(ApprovalNodeEntity::getSortOrder)
                    .orderByAsc(ApprovalNodeEntity::getCreatedAt))
                    .stream().map(this::toNodeResp).toList());
        }
        return dto;
    }

    private ApprovalNodeRespDTO toNodeResp(ApprovalNodeEntity entity) {
        ApprovalNodeRespDTO dto = new ApprovalNodeRespDTO();
        dto.setId(entity.getId());
        dto.setDefinitionId(entity.getDefinitionId());
        dto.setNodeCode(entity.getNodeCode());
        dto.setNodeName(entity.getNodeName());
        dto.setNodeType(entity.getNodeType());
        dto.setApproveMode(entity.getApproveMode());
        dto.setAssigneeType(entity.getAssigneeType());
        dto.setAssigneeValue(entity.getAssigneeValue());
        dto.setSortOrder(entity.getSortOrder());
        dto.setNextNodeCode(entity.getNextNodeCode());
        dto.setRejectToNodeCode(entity.getRejectToNodeCode());
        dto.setConditions(approvalConditionMapper.selectList(Wrappers.<ApprovalConditionEntity>lambdaQuery()
                .eq(ApprovalConditionEntity::getDefinitionId, entity.getDefinitionId())
                .eq(ApprovalConditionEntity::getNodeId, entity.getId())
                .eq(ApprovalConditionEntity::getDeleted, false)
                .orderByAsc(ApprovalConditionEntity::getSortOrder)
                .orderByAsc(ApprovalConditionEntity::getCreatedAt))
                .stream().map(this::toConditionResp).toList());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private ApprovalConditionRespDTO toConditionResp(ApprovalConditionEntity entity) {
        ApprovalConditionRespDTO dto = new ApprovalConditionRespDTO();
        dto.setId(entity.getId());
        dto.setDefinitionId(entity.getDefinitionId());
        dto.setNodeId(entity.getNodeId());
        dto.setConditionCode(entity.getConditionCode());
        dto.setFieldName(entity.getFieldName());
        dto.setOperator(entity.getOperator());
        dto.setCompareValue(entity.getCompareValue());
        dto.setTargetNodeCode(entity.getTargetNodeCode());
        dto.setSortOrder(entity.getSortOrder());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private void recordDefinitionAudit(Long definitionId, String operationType, Map<String, Object> afterData) {
        auditService.record(AuditRecordCommand.builder()
                .moduleCode(AuditModuleCodeEnum.WORKFLOW.getCode())
                .bizType(MODULE_WORKFLOW)
                .bizId(definitionId)
                .operationType(operationType)
                .afterData(afterData)
                .build());
    }
}
