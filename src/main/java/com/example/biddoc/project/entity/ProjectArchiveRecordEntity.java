package com.example.biddoc.project.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 项目归档记录，保存一次归档动作的汇总信息和快照校验入口。
 */
@Data
@TableName("bid_project_archive_record")
public class ProjectArchiveRecordEntity {

    @TableId
    private Long id;

    private Long projectId;

    private String archiveNo;

    private String archiveStatus;

    private OffsetDateTime archivedAt;

    private Long archivedBy;

    private String archiveReason;

    /**
     * 汇总字段用于归档列表快速展示，真实明细以清单和文档快照表为准。
     */
    private Integer checklistTotal;

    private Integer checklistComplete;

    private Integer documentTotal;

    private String snapshotHash;

    private String remark;

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
