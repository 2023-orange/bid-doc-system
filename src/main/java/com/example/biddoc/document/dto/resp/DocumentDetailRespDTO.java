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
     * 资料编号
     */
    private String documentNo;

    /**
     * 资料生命周期状态
     */
    private String documentStatus;

    /**
     * 业务分类
     */
    private String businessCategory;

    /**
     * 敏感等级
     */
    private String sensitiveLevel;

    /**
     * 是否存在有效期
     */
    private Boolean hasExpireDate;

    /**
     * 失效时间
     */
    private OffsetDateTime expireDate;

    /**
     * 元数据是否已补全
     */
    private Boolean metadataCompleted;

    /**
     * 当前版本号
     */
    private Integer currentVersionNo;

    /**
     * 当前资料审批状态，等同于 documentStatus，方便前端审批视图展示
     */
    private String approvalStatus;

    /**
     * 当前版本审批状态
     */
    private String versionApprovalStatus;

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
