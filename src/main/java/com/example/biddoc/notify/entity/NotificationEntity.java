package com.example.biddoc.notify.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sys_notification")
public class NotificationEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long receiverUserId;
    private String type;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private Boolean readFlag;
    private OffsetDateTime readAt;

    @TableLogic
    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;
}
