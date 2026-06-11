package com.example.biddoc.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 文档下载日志实体
 */
@Data
@TableName("doc_download_log")
public class DownloadLogEntity {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文档 ID
     */
    private Long documentId;

    /**
     * 下载的版本号
     */
    private Integer versionNo;

    /**
     * 下载用户 ID
     */
    private Long userId;

    /**
     * 下载时间
     */
    private OffsetDateTime downloadTime;

    /**
     * IP 地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Boolean deleted;
}
