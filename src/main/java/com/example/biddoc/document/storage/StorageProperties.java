package com.example.biddoc.document.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 存储配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * 存储类型：local（本地磁盘）或 minio（MinIO 对象存储）
     */
    private String type = "local";

    /**
     * 本地磁盘存储配置
     */
    private LocalConfig local = new LocalConfig();

    @Data
    public static class LocalConfig {
        /**
         * 本地存储根目录
         */
        private String root = "./.local-storage";
    }
}
