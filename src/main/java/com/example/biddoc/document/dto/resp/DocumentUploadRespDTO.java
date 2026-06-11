package com.example.biddoc.document.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档上传响应 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentUploadRespDTO {

    /**
     * 文档 ID
     */
    private String documentId;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * MIME 类型
     */
    private String mimeType;
}
