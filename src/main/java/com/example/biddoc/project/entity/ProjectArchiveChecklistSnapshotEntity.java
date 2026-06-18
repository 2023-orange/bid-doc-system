package com.example.biddoc.project.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 项目归档清单快照，固化归档时清单项的要求、状态与绑定数量。
 */
@Data
@TableName("bid_project_archive_checklist_snapshot")
public class ProjectArchiveChecklistSnapshotEntity {

    @TableId
    private Long id;

    private Long archiveRecordId;

    private Long projectId;

    private Long checklistItemId;

    private Long templateItemId;

    private String itemName;

    private Boolean requiredFlag;

    private String itemStatus;

    private Integer boundDocumentCount;

    /**
     * 保留扩展快照内容，避免后续清单模型变化影响历史归档可追溯性。
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
