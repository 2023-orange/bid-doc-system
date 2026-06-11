package com.example.biddoc.document.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 文档详情响应 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentDetailRespDTO {

    /**
     * 文档 ID
     */
    private String id;

    /**
     * 所属文件夹 ID
     */
    private String folderId;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 当前版本号
     */
    private Integer currentVersionNo;

    /**
     * 当前版本详情
     */
    private DocumentVersionRespDTO currentVersion;

    /**
     * 所有者用户 ID
     */
    private String ownerUserId;

    /**
     * 所有者部门 ID
     */
    private String ownerDeptId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    private OffsetDateTime updatedAt;
}
