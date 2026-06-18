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
@TableName("wf_approval_action_log")
public class ApprovalActionLogEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long instanceId;
    private Long taskId;
    private Long definitionId;
    private Long nodeId;
    private String nodeCode;
    private String actionType;
    private Long actionUserId;
    private String actionComment;
    private OffsetDateTime actionAt;
    private String beforeStatus;
    private String afterStatus;

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
