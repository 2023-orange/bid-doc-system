package com.example.biddoc.project.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("bid_project_checklist_document")
public class ProjectChecklistDocumentEntity {
    @TableId
    private Long id;
    private Long checklistItemId;
    private Long documentId;
    private Integer versionNo;
    private String bindType;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
    @TableField(fill = FieldFill.INSERT)
    private String createdBy;
    @TableLogic
    private Boolean deleted;
}
