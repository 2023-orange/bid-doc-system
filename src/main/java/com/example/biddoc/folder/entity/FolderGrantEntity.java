package com.example.biddoc.folder.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("doc_folder_grant")
public class FolderGrantEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long folderId;
    private String subjectType;
    private String subjectId;
    private String permissionCode;
    private String grantScope;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
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
