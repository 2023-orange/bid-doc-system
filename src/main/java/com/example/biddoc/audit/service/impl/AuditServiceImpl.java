package com.example.biddoc.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.dto.req.AuditQueryReqDTO;
import com.example.biddoc.audit.dto.resp.AuditLogRespDTO;
import com.example.biddoc.audit.entity.AuditOperationLogEntity;
import com.example.biddoc.audit.mapper.AuditOperationLogMapper;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.common.result.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    /** MDC 中 traceId 的 key，与 LoggingInterceptor 保持一致 */
    private static final String MDC_TRACE_ID = "traceId";

    private final AuditOperationLogMapper auditOperationLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void record(AuditRecordCommand command) {
        // 必填字段校验：缺失即抛业务异常，与主事务一起回滚，避免审计漏记
        validateRequired(command);

        AuditOperationLogEntity entity = new AuditOperationLogEntity();
        entity.setModuleCode(command.getModuleCode());
        entity.setBizType(command.getBizType());
        entity.setBizId(command.getBizId());
        entity.setOperationType(command.getOperationType());
        entity.setBeforeData(command.getBeforeData());
        entity.setAfterData(command.getAfterData());
        entity.setExtraData(command.getExtraData());
        entity.setOperationTime(OffsetDateTime.now());
        entity.setDeleted(Boolean.FALSE);

        // 操作者信息从当前用户上下文取，UserContext 为空时（如定时任务/系统调用）允许 null
        UserContext.UserInfo user = UserContext.get();
        if (user != null) {
            entity.setOperatorUserId(user.getUserId());
            entity.setOperatorDeptId(user.getDeptId());
        }

        // 链路追踪：与 LoggingInterceptor 写入的 traceId 关联，MDC 缺失时容忍 null
        String traceId = MDC.get(MDC_TRACE_ID);
        if (traceId != null && !traceId.isEmpty()) {
            entity.setRequestId(traceId);
        }

        auditOperationLogMapper.insert(entity);
    }

    @Override
    public PageResponse<AuditLogRespDTO> queryLogs(AuditQueryReqDTO req) {
        UserContext.UserInfo user = UserContext.get();
        if (user == null || !user.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_MATCH, "仅超级管理员可查询审计日志");
        }

        AuditQueryReqDTO query = req != null ? req : new AuditQueryReqDTO();
        long pageNo = normalizePage(query.getPage());
        long pageSize = normalizeSize(query.getSize());

        LambdaQueryWrapper<AuditOperationLogEntity> wrapper = new LambdaQueryWrapper<AuditOperationLogEntity>()
                .eq(AuditOperationLogEntity::getDeleted, false)
                .eq(StringUtils.hasText(query.getModuleCode()), AuditOperationLogEntity::getModuleCode, query.getModuleCode())
                .eq(StringUtils.hasText(query.getBizType()), AuditOperationLogEntity::getBizType, query.getBizType())
                .eq(query.getBizId() != null, AuditOperationLogEntity::getBizId, query.getBizId())
                .eq(StringUtils.hasText(query.getOperationType()), AuditOperationLogEntity::getOperationType, query.getOperationType())
                .eq(query.getOperatorUserId() != null, AuditOperationLogEntity::getOperatorUserId, query.getOperatorUserId())
                .eq(query.getOperatorDeptId() != null, AuditOperationLogEntity::getOperatorDeptId, query.getOperatorDeptId())
                .ge(query.getStartTime() != null, AuditOperationLogEntity::getOperationTime, query.getStartTime())
                .le(query.getEndTime() != null, AuditOperationLogEntity::getOperationTime, query.getEndTime())
                .orderByDesc(AuditOperationLogEntity::getOperationTime);

        Page<AuditOperationLogEntity> page = auditOperationLogMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        Page<AuditLogRespDTO> dtoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(page.getRecords().stream().map(this::toRespDTO).toList());
        return PageResponse.of(dtoPage);
    }

    private void validateRequired(AuditRecordCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.AUDIT_RECORD_FAILED, "审计命令不能为空");
        }
        if (isBlank(command.getModuleCode())
                || isBlank(command.getBizType())
                || isBlank(command.getOperationType())
                || command.getBizId() == null) {
            log.warn("审计命令必填字段缺失: module={}, bizType={}, bizId={}, operationType={}",
                    command.getModuleCode(), command.getBizType(), command.getBizId(), command.getOperationType());
            throw new BusinessException(ErrorCode.AUDIT_RECORD_FAILED, "审计命令必填字段缺失");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private long normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private AuditLogRespDTO toRespDTO(AuditOperationLogEntity entity) {
        AuditLogRespDTO dto = new AuditLogRespDTO();
        dto.setId(entity.getId());
        dto.setModuleCode(entity.getModuleCode());
        dto.setBizType(entity.getBizType());
        dto.setBizId(entity.getBizId());
        dto.setOperationType(entity.getOperationType());
        dto.setOperatorUserId(entity.getOperatorUserId());
        dto.setOperatorDeptId(entity.getOperatorDeptId());
        dto.setRequestId(entity.getRequestId());
        dto.setOperationTime(entity.getOperationTime());
        dto.setBeforeData(entity.getBeforeData());
        dto.setAfterData(entity.getAfterData());
        dto.setExtraData(entity.getExtraData());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        return dto;
    }
}
