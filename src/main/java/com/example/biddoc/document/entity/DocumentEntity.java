package com.example.biddoc.document.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 文档主表实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("doc_document")
public class DocumentEntity {

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 所属文件夹 ID，不允许为 0（根级不能直接挂文档）
     */
    private Long folderId;

    /**
     * 业务文件名，同 folder 下唯一
     */
    private String name;

    /**
     * 当前指向的版本号，与 doc_document_version.version_no 对应
     */
    private Integer currentVersionNo;

    /**
     * 当前版本字节数（冗余，列表免 JOIN）
     */
    private Long latestSize;

    /**
     * 当前版本 MIME（冗余）
     */
    private String latestMime;

    /**
     * 首次上传者，后续版本变更不修改此字段
     */
    private Long ownerUserId;

    /**
     * 首次上传者部门（冗余，过滤用）
     */
    private Long ownerDeptId;

    /**
     * 状态，预留（1=正常）
     */
    private Integer status;

    /**
     * 资料业务状态：上传后先进入待补全，后续审批回写正式状态。
     */
    private String documentStatus;

    private String documentNo;
    private String businessCategory;
    private String tenderStructureCategory;
    private String sensitiveLevel;
    private String sourceType;
    private OffsetDateTime effectiveDate;
    private OffsetDateTime expireDate;
    private Boolean hasExpireDate;
    private Boolean metadataCompleted;
    private String invalidReason;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Boolean deleted;
}
