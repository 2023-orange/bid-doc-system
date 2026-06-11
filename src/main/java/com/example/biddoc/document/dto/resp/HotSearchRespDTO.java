package com.example.biddoc.document.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 热门搜索响应 DTO
 */
@Data
@Schema(description = "热门搜索响应")
public class HotSearchRespDTO {

    @Schema(description = "搜索关键词")
    private String keyword;

    @Schema(description = "搜索次数")
    private Long searchCount;
}
