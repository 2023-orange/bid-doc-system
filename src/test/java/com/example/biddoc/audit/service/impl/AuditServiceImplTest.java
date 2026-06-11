package com.example.biddoc.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.audit.dto.req.AuditQueryReqDTO;
import com.example.biddoc.audit.entity.AuditOperationLogEntity;
import com.example.biddoc.audit.mapper.AuditOperationLogMapper;
import com.example.biddoc.auth.constant.RoleCodeEnum;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceImplTest {

    private final AuditOperationLogMapper auditOperationLogMapper = mock(AuditOperationLogMapper.class);
    private final AuditServiceImpl service = new AuditServiceImpl(auditOperationLogMapper);

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void queryLogsRejectsNonSuperAdmin() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of(RoleCodeEnum.EMPLOYEE.getCode()), 100L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.queryLogs(new AuditQueryReqDTO()));

        assertEquals(ErrorCode.ROLE_NOT_MATCH, ex.getErrorCode());
    }

    @Test
    void queryLogsReturnsPagedLogsForSuperAdmin() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of(RoleCodeEnum.SUPER_ADMIN.getCode()), 1L));

        AuditOperationLogEntity entity = new AuditOperationLogEntity();
        entity.setId(10L);
        entity.setModuleCode("DOCUMENT");
        entity.setBizType("DOCUMENT");
        entity.setBizId(1000L);
        entity.setOperationType("DOWNLOAD");
        entity.setOperatorUserId(7L);
        entity.setOperationTime(OffsetDateTime.now());

        Page<AuditOperationLogEntity> page = new Page<>(1, 20);
        page.setRecords(List.of(entity));
        page.setTotal(1);
        when(auditOperationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        AuditQueryReqDTO req = new AuditQueryReqDTO();
        req.setModuleCode("DOCUMENT");
        req.setOperationType("DOWNLOAD");

        var result = service.queryLogs(req);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("DOCUMENT", result.getList().get(0).getModuleCode());
        verify(auditOperationLogMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }
}
