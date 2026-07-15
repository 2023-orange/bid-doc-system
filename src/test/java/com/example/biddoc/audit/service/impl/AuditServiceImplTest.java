package com.example.biddoc.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.audit.dto.req.AuditQueryReqDTO;
import com.example.biddoc.audit.entity.AuditOperationLogEntity;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysUserMapper;
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
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final AuditServiceImpl service = new AuditServiceImpl(auditOperationLogMapper, sysUserMapper);

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
        entity.setObjectName("投标文件.pdf");
        entity.setActionSummary("张三 下载了《投标文件.pdf》 v3");
        entity.setRelatedBizType("APPROVAL_INSTANCE");
        entity.setRelatedBizId(9001L);
        entity.setClientIp("127.0.0.1");
        entity.setUserAgent("JUnit");
        entity.setOperationTime(OffsetDateTime.now());

        SysUser operator = new SysUser();
        operator.setId(7L);
        operator.setRealName("张三");

        Page<AuditOperationLogEntity> page = new Page<>(1, 20);
        page.setRecords(List.of(entity));
        page.setTotal(1);
        when(auditOperationLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(operator));

        AuditQueryReqDTO req = new AuditQueryReqDTO();
        req.setModuleCode("DOCUMENT");
        req.setOperationType("DOWNLOAD");
        req.setRelatedBizType("APPROVAL_INSTANCE");
        req.setRelatedBizId(9001L);
        req.setKeyword("投标");

        var result = service.queryLogs(req);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("DOCUMENT", result.getList().get(0).getModuleCode());
        assertEquals("投标文件.pdf", result.getList().get(0).getObjectName());
        assertEquals("张三 下载了《投标文件.pdf》 v3", result.getList().get(0).getActionSummary());
        assertEquals("APPROVAL_INSTANCE", result.getList().get(0).getRelatedBizType());
        assertEquals(9001L, result.getList().get(0).getRelatedBizId());
        assertEquals("127.0.0.1", result.getList().get(0).getClientIp());
        assertEquals("JUnit", result.getList().get(0).getUserAgent());
        assertEquals("张三", result.getList().get(0).getOperatorName());
        verify(auditOperationLogMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    void queryLogDetailReturnsSingleLogWithOperatorName() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of(RoleCodeEnum.SUPER_ADMIN.getCode()), 1L));

        AuditOperationLogEntity entity = new AuditOperationLogEntity();
        entity.setId(20L);
        entity.setModuleCode("DOCUMENT");
        entity.setBizType("DOCUMENT");
        entity.setBizId(1000L);
        entity.setOperationType("PREVIEW");
        entity.setOperatorUserId(8L);
        entity.setObjectName("资格预审.pdf");
        entity.setActionSummary("李四 预览了《资格预审.pdf》 v1");

        SysUser operator = new SysUser();
        operator.setId(8L);
        operator.setRealName("李四");

        when(auditOperationLogMapper.selectById(20L)).thenReturn(entity);
        when(sysUserMapper.selectBatchIds(any())).thenReturn(List.of(operator));

        var result = service.queryLogDetail(20L);

        assertEquals(20L, result.getId());
        assertEquals("资格预审.pdf", result.getObjectName());
        assertEquals("李四 预览了《资格预审.pdf》 v1", result.getActionSummary());
        assertEquals("李四", result.getOperatorName());
    }

    @Test
    void queryTimelineReturnsAscendingLogsForSuperAdmin() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of(RoleCodeEnum.SUPER_ADMIN.getCode()), 1L));

        AuditOperationLogEntity entity = new AuditOperationLogEntity();
        entity.setId(30L);
        entity.setModuleCode("DOCUMENT");
        entity.setBizType("DOCUMENT");
        entity.setBizId(1000L);
        entity.setOperationType("UPLOAD");
        entity.setObjectName("投标文件.pdf");
        entity.setActionSummary("王五 上传了《投标文件.pdf》");
        entity.setOperationTime(OffsetDateTime.now());

        when(auditOperationLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(entity));

        AuditQueryReqDTO req = new AuditQueryReqDTO();
        req.setBizType("DOCUMENT");
        req.setBizId(1000L);

        var result = service.queryTimeline(req);

        assertEquals(1, result.size());
        assertEquals("UPLOAD", result.get(0).getOperationType());
        assertEquals("投标文件.pdf", result.get(0).getObjectName());
        verify(auditOperationLogMapper).selectList(any(LambdaQueryWrapper.class));
    }
}
