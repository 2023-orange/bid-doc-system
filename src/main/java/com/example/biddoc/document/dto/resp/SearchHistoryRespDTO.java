package com.example.biddoc.document.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 搜索历史响应 DTO
 */
@Data
@Schema(description = "搜索历史响应")
public class SearchHistoryRespDTO {

    @Schema(description = "历史记录ID")
    private String id;

    @Schema(description = "搜索关键词")
    private String keyword;

    @Schema(description = "文件夹ID（可选）")
    private String folderId;

    @Schema(description = "搜索结果数量")
    private Integer resultCount;

    @Schema(description = "搜索时间")
    private OffsetDateTime searchTime;
}
