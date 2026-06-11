package com.example.biddoc.document.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 文档版本表实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("doc_document_version")
public class DocumentVersionEntity {

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 文档 ID
     */
    private Long documentId;

    /**
     * 版本号，从 1 单调递增，同 document_id 下唯一
     */
    private Integer versionNo;

    /**
     * 存储 key，LocalDisk 实现为相对路径，MinIO 实现为 object key
     */
    private String storageKey;

    /**
     * 字节数，与存储中的物理大小一致
     */
    private Long size;

    /**
     * 服务端通过 Apache Tika 探测的 MIME，不信任客户端 Content-Type
     */
    private String mimeType;

    /**
     * 上传时原始文件名，用于下载默认 filename
     */
    private String originalFilename;

    /**
     * SHA-256 十六进制，MVP 计算并存，不做去重（预留二期使用）
     */
    private String contentHash;

    /**
     * 上传者用户 ID
     */
    private Long uploadedByUserId;

    /**
     * 版本变更说明
     */
    private String changeLog;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Boolean deleted;
}
