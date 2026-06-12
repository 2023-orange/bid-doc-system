package com.example.biddoc.document.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 文档搜索请求 DTO
 */
@Data
@Schema(description = "文档搜索请求")
public class DocumentSearchReqDTO {

    /**
     * 搜索关键词（文档名称模糊匹配）
     * 为空时返回所有文档
     */
    @Schema(description = "搜索关键词（文档名称模糊匹配）", example = "合同")
    private String keyword;

    /**
     * 文件夹ID（可选）
     * 指定后只搜索该文件夹及其子文件夹下的文档
     */
    @Schema(description = "文件夹ID（可选，指定后只搜索该文件夹及其子文件夹）", example = "123456")
    private Long folderId;

    /**
     * 是否递归搜索子文件夹
     * true: 搜索指定文件夹及其所有子文件夹
     * false: 仅搜索指定文件夹（不包含子文件夹）
     * 默认: true
     */
    @Schema(description = "是否递归搜索子文件夹", example = "true")
    private Boolean recursive = true;

    @Schema(description = "MIME 类型过滤", example = "application/pdf")
    private String mimeType;

    @Schema(description = "文档所有者用户 ID", example = "123456")
    private Long ownerUserId;

    @Schema(description = "创建时间起点")
    private OffsetDateTime createdFrom;

    @Schema(description = "创建时间终点")
    private OffsetDateTime createdTo;

    @Schema(description = "仅搜索当前用户收藏文件夹下的文档", example = "false")
    private Boolean favoriteFolderOnly = false;

    @Schema(description = "标签 ID 列表")
    private List<Long> tagIds;

    @Schema(description = "资料编号")
    private String documentNo;

    @Schema(description = "资料状态")
    private String documentStatus;

    @Schema(description = "资料业务分类")
    private String businessCategory;

    @Schema(description = "敏感等级")
    private String sensitiveLevel;

    @Schema(description = "归属部门 ID")
    private Long ownerDeptId;

    @Schema(description = "仅查询已过期资料")
    private Boolean expiredOnly = false;

    /**
     * 排序字段
     * 可选值: name, size, createdAt, updatedAt
     * 默认: createdAt
     */
    @Schema(description = "排序字段（name/size/createdAt/updatedAt）", example = "createdAt")
    private String sortBy = "createdAt";

    /**
     * 排序方向
     * 可选值: asc, desc
     * 默认: desc
     */
    @Schema(description = "排序方向（asc/desc）", example = "desc")
    private String sortOrder = "desc";

    /**
     * 页码（从1开始）
     */
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;
}
