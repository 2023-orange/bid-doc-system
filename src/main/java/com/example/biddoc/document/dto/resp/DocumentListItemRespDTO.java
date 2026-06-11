package com.example.biddoc.document.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 文档列表项响应 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentListItemRespDTO {

    /**
     * 文档 ID
     */
    private String id;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 当前版本号
     */
    private Integer currentVersionNo;

    /**
     * 当前版本文件大小（字节）
     */
    private Long latestSize;

    /**
     * 当前版本 MIME 类型
     */
    private String latestMime;

    /**
     * 所有者用户 ID
     */
    private String ownerUserId;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;
}
