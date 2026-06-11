package com.example.biddoc.document.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 存储配置属性
 *
 * 从 application.yml 的 storage.minio 节点读取配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage.minio")
public class MinioProperties {

    /**
     * MinIO 服务端点 (例如: http://localhost:9000)
     */
    private String endpoint;

    /**
     * 访问密钥 (Access Key)
     */
    private String accessKey;

    /**
     * 秘密密钥 (Secret Key)
     */
    private String secretKey;

    /**
     * 存储桶名称 (Bucket Name)
     * 所有文档文件将存储在此桶中
     */
    private String bucketName;
}
