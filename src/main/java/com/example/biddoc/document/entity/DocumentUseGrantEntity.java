package com.example.biddoc.document.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 资料使用授权记录，固定到具体资料版本，避免授权后因版本切换产生权限漂移。
 */
@Data
@TableName("doc_document_use_grant")
public class DocumentUseGrantEntity {

    @TableId
    private Long id;

    private Long documentId;

    private Integer versionNo;

    private Long projectId;

    /**
     * 记录申请人与被授权人，后续审批回写和权限判断都依赖这两个身份边界。
     */
    private Long applicantId;

    private Long granteeId;

    private Long approvalInstanceId;

    private String grantType;

    private String scenario;

    private OffsetDateTime validFrom;

    private OffsetDateTime validUntil;

    private String status;

    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableLogic
    private Boolean deleted;
}
