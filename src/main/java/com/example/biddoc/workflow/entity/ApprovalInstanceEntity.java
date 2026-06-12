package com.example.biddoc.workflow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("wf_approval_instance")
public class ApprovalInstanceEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long documentId;
    private String bizModule;
    private String bizType;
    private Long bizId;
    private String scenario;
    private Long submitterUserId;
    private String status;
    private String submitComment;
    private OffsetDateTime submittedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime finishedAt;

    @TableLogic
    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
