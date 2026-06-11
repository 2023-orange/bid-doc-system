package com.example.biddoc.document.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 热门文档响应 DTO
 */
@Data
@Schema(description = "热门文档响应")
public class PopularDocumentRespDTO {

    @Schema(description = "文档ID")
    private String id;

    @Schema(description = "文档名称")
    private String name;

    @Schema(description = "当前版本号")
    private Integer currentVersionNo;

    @Schema(description = "文件大小")
    private String size;

    @Schema(description = "MIME类型")
    private String mimeType;

    @Schema(description = "所有者用户ID")
    private String ownerUserId;

    @Schema(description = "下载次数")
    private Long downloadCount;

    @Schema(description = "创建时间")
    private OffsetDateTime createdAt;
}
