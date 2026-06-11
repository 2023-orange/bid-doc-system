package com.example.biddoc.document.storage.impl;

import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.storage.StorageAdapter;
import com.example.biddoc.document.storage.config.MinioProperties;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * MinIO 对象存储适配器实现
 *
 * 仅在 storage.type=minio 时生效
 * 负责将文档文件存储到 MinIO 对象存储服务
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioStorageAdapter implements StorageAdapter {

    private final MinioProperties minioProperties;
    private MinioClient minioClient;

    public MinioStorageAdapter(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    /**
     * 初始化 MinIO 客户端并确保存储桶存在
     */
    @PostConstruct
    public void init() {
        try {
            // 构建 MinIO 客户端
            minioClient = MinioClient.builder()
                    .endpoint(minioProperties.getEndpoint())
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    .build();

            // 检查存储桶是否存在，不存在则创建
            String bucketName = minioProperties.getBucketName();
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("MinIO 存储桶已创建: {}", bucketName);
            } else {
                log.info("MinIO 存储桶已存在: {}", bucketName);
            }

            log.info("MinIO 存储适配器初始化成功，端点: {}, 存储桶: {}",
                    minioProperties.getEndpoint(), bucketName);

        } catch (Exception e) {
            log.error("MinIO 存储适配器初始化失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "MinIO 初始化失败: " + e.getMessage());
        }
    }

    /**
     * 存储文件到 MinIO
     *
     * @param in          文件输入流
     * @param size        文件大小（字节）
     * @param contentType MIME 类型（可选）
     * @param hintExt     文件扩展名提示（可选）
     * @return 存储键（格式: yyyy/MM/dd/objectName）
     */
    @Override
    public String put(InputStream in, long size, String contentType, String hintExt) {
        try {
            // 生成存储键：yyyy/MM/dd/雪花ID
            String storageKey = generateStorageKey(hintExt);

            // 构建上传参数
            PutObjectArgs.Builder argsBuilder = PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(storageKey)
                    .stream(in, size, -1); // -1 表示使用默认的 part size

            // 如果提供了 contentType，则设置
            if (contentType != null && !contentType.isEmpty()) {
                argsBuilder.contentType(contentType);
            }

            // 执行上传
            minioClient.putObject(argsBuilder.build());

            log.debug("文件已上传到 MinIO: storageKey={}, size={}", storageKey, size);
            return storageKey;

        } catch (Exception e) {
            log.error("MinIO 文件上传失败: size={}", size, e);
            throw new BusinessException(ErrorCode.STORAGE_WRITE_FAILED, "MinIO 上传失败: " + e.getMessage());
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                log.warn("关闭输入流失败", e);
            }
        }
    }

    /**
     * 从 MinIO 获取文件输入流
     *
     * @param storageKey 存储键
     * @return 文件输入流
     */
    @Override
    public InputStream get(String storageKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(storageKey)
                            .build()
            );
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new BusinessException(ErrorCode.STORAGE_OBJECT_NOT_FOUND, "文件不存在: " + storageKey);
            }
            log.error("MinIO 文件读取失败: storageKey={}", storageKey, e);
            throw new BusinessException(ErrorCode.STORAGE_READ_FAILED, "MinIO 读取失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("MinIO 文件读取失败: storageKey={}", storageKey, e);
            throw new BusinessException(ErrorCode.STORAGE_READ_FAILED, "MinIO 读取失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件大小
     *
     * @param storageKey 存储键
     * @return 文件大小（字节）
     */
    @Override
    public long size(String storageKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(storageKey)
                            .build()
            );
            return stat.size();
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new BusinessException(ErrorCode.STORAGE_OBJECT_NOT_FOUND, "文件不存在: " + storageKey);
            }
            log.error("MinIO 获取文件大小失败: storageKey={}", storageKey, e);
            throw new BusinessException(ErrorCode.STORAGE_READ_FAILED, "MinIO 获取大小失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("MinIO 获取文件大小失败: storageKey={}", storageKey, e);
            throw new BusinessException(ErrorCode.STORAGE_READ_FAILED, "MinIO 获取大小失败: " + e.getMessage());
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param storageKey 存储键
     * @return true=存在, false=不存在
     */
    @Override
    public boolean exists(String storageKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(storageKey)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            log.error("MinIO 检查文件存在性失败: storageKey={}", storageKey, e);
            throw new BusinessException(ErrorCode.STORAGE_READ_FAILED, "MinIO 检查失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("MinIO 检查文件存在性失败: storageKey={}", storageKey, e);
            throw new BusinessException(ErrorCode.STORAGE_READ_FAILED, "MinIO 检查失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件（软删除，实际不删除，仅标记）
     * MinIO 不支持软删除，此方法暂不实现物理删除
     *
     * @param storageKey 存储键
     */
    @Override
    public void delete(String storageKey) {
        // MinIO 不支持软删除，文档模块已通过 deleted 字段实现软删除
        // 此方法预留，未来可实现物理删除或移动到回收桶
        log.debug("MinIO 删除操作（暂不实际删除）: storageKey={}", storageKey);
    }

    /**
     * 生成存储键
     * 格式: yyyy/MM/dd/雪花ID.ext
     *
     * @param hintExt 文件扩展名提示（可选）
     * @return 存储键
     */
    private String generateStorageKey(String hintExt) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        long snowflakeId = com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();

        if (hintExt != null && !hintExt.isEmpty()) {
            String normalizedExt = hintExt.startsWith(".") ? hintExt : "." + hintExt;
            return datePath + "/" + snowflakeId + normalizedExt;
        } else {
            return datePath + "/" + snowflakeId;
        }
    }
}
