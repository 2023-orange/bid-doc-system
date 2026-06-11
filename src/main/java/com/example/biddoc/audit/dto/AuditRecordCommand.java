package com.example.biddoc.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 审计记录写入命令。
 *
 * <p>beforeData/afterData/extraData 设计为 {@code Map<String,Object>}，以匹配
 * {@code AuditOperationLogEntity} 上的 JacksonTypeHandler；调用方放入复杂类型
 * （如 OffsetDateTime）时应先转换为字符串等可被 Jackson 直接序列化的形态，避免
 * jsonb 写入失败或反序列化偏差。
 */
@Data
@Builder
public class AuditRecordCommand {

    /** 模块编码，例如 FOLDER */
    private String moduleCode;

    /** 业务类型，folder 模块统一传 FOLDER */
    private String bizType;

    /** 业务主键，folder 模块统一传 folderId */
    private Long bizId;

    /** 操作类型，取值参考 AuditOperationTypeEnum */
    private String operationType;

    /** 变更前快照 */
    private Map<String, Object> beforeData;

    /** 变更后快照 */
    private Map<String, Object> afterData;

    /** 附加信息，例如 grantId/managerId、子树数量等 */
    private Map<String, Object> extraData;
}
