package com.example.biddoc.document.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文档类型分布响应 DTO
 */
@Data
@Schema(description = "文档类型分布响应")
public class DocumentTypeStatsRespDTO {

    @Schema(description = "MIME类型")
    private String mimeType;

    @Schema(description = "文档数量")
    private Long count;

    @Schema(description = "总大小（字节）")
    private Long totalSize;

    @Schema(description = "总大小（可读格式）")
    private String totalSizeReadable;
}
