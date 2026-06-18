package com.example.biddoc.project.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 项目归档文档快照，固定归档时文档版本和关键元数据。
 */
@Data
@TableName("bid_project_archive_document_snapshot")
public class ProjectArchiveDocumentSnapshotEntity {

    @TableId
    private Long id;

    private Long archiveRecordId;

    private Long projectId;

    private Long checklistItemId;

    private Long documentId;

    private Integer versionNo;

    private String documentName;

    private String documentStatus;

    private String versionStatus;

    private OffsetDateTime expireAt;

    private String storageType;

    private Long fileSize;

    /**
     * 保留文档扩展快照内容，保护历史归档不受当前资料元数据变更影响。
     */
    private String snapshotJson;

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
