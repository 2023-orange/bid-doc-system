package com.example.biddoc.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 文档搜索历史实体
 */
@Data
@TableName("doc_search_history")
public class SearchHistoryEntity {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 搜索用户 ID
     */
    private Long userId;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 搜索范围文件夹 ID（可选）
     */
    private Long folderId;

    /**
     * 搜索结果数量
     */
    private Integer resultCount;

    /**
     * 搜索时间
     */
    private OffsetDateTime searchTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Boolean deleted;
}
