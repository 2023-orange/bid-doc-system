package com.example.biddoc.document.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 文档版本响应 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentVersionRespDTO {

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * MIME 类型
     */
    private String mimeType;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 上传者用户 ID
     */
    private String uploadedByUserId;

    /**
     * 上传时间
     */
    private OffsetDateTime uploadedAt;

    /**
     * 版本变更说明
     */
    private String changeLog;
}
