package com.example.biddoc.document.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 存储空间统计响应 DTO
 */
@Data
@Schema(description = "存储空间统计响应")
public class StorageStatsRespDTO {

    @Schema(description = "文档总数")
    private Long totalDocuments;

    @Schema(description = "版本总数")
    private Long totalVersions;

    @Schema(description = "总存储空间（字节）")
    private Long totalSize;

    @Schema(description = "总存储空间（可读格式，如 1.5 GB）")
    private String totalSizeReadable;

    @Schema(description = "当前用户的文档数")
    private Long myDocuments;

    @Schema(description = "当前用户的存储空间（字节）")
    private Long mySize;

    @Schema(description = "当前用户的存储空间（可读格式）")
    private String mySizeReadable;
}
