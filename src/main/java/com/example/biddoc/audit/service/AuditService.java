package com.example.biddoc.audit.service;

import com.example.biddoc.audit.dto.AuditRecordCommand;
import com.example.biddoc.audit.dto.req.AuditQueryReqDTO;
import com.example.biddoc.audit.dto.resp.AuditLogRespDTO;
import com.example.biddoc.common.result.PageResponse;

/**
 * 审计写入入口。MVP 阶段采用同事务写入，调用方异常应直接传播以触发整体回滚，
 * 保证业务记录与审计记录的强一致；后续若有性能需要，再切换为事件/异步。
 */
public interface AuditService {

    /** 写入一条审计日志，与调用方同事务执行。 */
    void record(AuditRecordCommand command);

    /** 分页查询审计日志，v1 仅 SUPER_ADMIN 可使用。 */
    PageResponse<AuditLogRespDTO> queryLogs(AuditQueryReqDTO req);
}
