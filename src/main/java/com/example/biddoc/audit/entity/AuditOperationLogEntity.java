package com.example.biddoc.audit.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@TableName(value = "audit_operation_log", autoResultMap = true)
public class AuditOperationLogEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String moduleCode;
    private String bizType;
    private Long bizId;
    private String operationType;
    private Long operatorUserId;
    private Long operatorDeptId;
    private String requestId;
    private OffsetDateTime operationTime;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> beforeData;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> afterData;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraData;

    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;
}
