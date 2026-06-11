package com.example.biddoc.document.storage;

import com.example.biddoc.document.storage.impl.LocalDiskStorageAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储适配器配置
 */
@Configuration
@RequiredArgsConstructor
public class StorageConfig {

    private final StorageProperties storageProperties;

    /**
     * 本地磁盘存储适配器
     * 当 storage.type=local 或未配置时生效
     */
    @Bean
    @ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "local", matchIfMissing = true)
    public StorageAdapter localDiskStorageAdapter() {
        return new LocalDiskStorageAdapter(storageProperties.getLocal().getRoot());
    }
}
