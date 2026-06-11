package com.example.biddoc.document.storage.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.storage.StorageAdapter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地磁盘存储适配器实现
 */
@Slf4j
public class LocalDiskStorageAdapter implements StorageAdapter {

    private final Path rootPath;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public LocalDiskStorageAdapter(String root) {
        this.rootPath = Paths.get(root).toAbsolutePath().normalize();
    }

    /**
     * 启动时检查根目录，不存在则创建，无写权限则启动失败
     */
    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
                log.info("本地存储根目录已创建: {}", rootPath);
            }

            // 可写性校验：尝试创建临时文件
            Path testFile = rootPath.resolve(".write-test-" + System.currentTimeMillis());
            Files.createFile(testFile);
            Files.delete(testFile);

            log.info("本地存储适配器初始化成功，根目录: {}", rootPath);
        } catch (IOException e) {
            log.error("本地存储根目录初始化失败: {}", rootPath, e);
            throw new IllegalStateException("本地存储根目录不可写: " + rootPath, e);
        }
    }

    @Override
    public String put(InputStream in, long size, String contentType, String hintExt) {
        try {
            // 生成 storageKey: yyyy/MM/dd/<snowflakeId>
            long storageId = IdWorker.getId();
            String datePath = LocalDate.now().format(DATE_FORMATTER);
            String storageKey = datePath + "/" + storageId;

            // 计算物理路径
            Path targetPath = resolveAndValidatePath(storageKey);

            // 确保父目录存在
            Files.createDirectories(targetPath.getParent());

            // 写入文件
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 校验字节数
            long actualSize = Files.size(targetPath);
            if (actualSize != size) {
                Files.deleteIfExists(targetPath);
                throw new BusinessException(ErrorCode.STORAGE_WRITE_FAILED,
                    String.format("文件大小校验失败，期望 %d 字节，实际 %d 字节", size, actualSize));
            }

            log.debug("文件写入成功: storageKey={}, size={}", storageKey, size);
            return storageKey;

        } catch (IOException e) {
            log.error("文件存储写入失败: size={}", size, e);
            throw new BusinessException(ErrorCode.STORAGE_WRITE_FAILED, "文件存储写入失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream get(String storageKey) {
        try {
            Path targetPath = resolveAndValidatePath(storageKey);

            if (!Files.exists(targetPath)) {
                throw new BusinessException(ErrorCode.STORAGE_OBJECT_NOT_FOUND,
                    "存储对象不存在: " + storageKey);
            }

            return Files.newInputStream(targetPath);

        } catch (IOException e) {
            log.error("文件存储读取失败: storageKey={}", storageKey, e);
            throw new BusinessException(ErrorCode.STORAGE_READ_FAILED, "文件存储读取失败: " + e.getMessage());
        }
    }

    @Override
    public long size(String storageKey) {
        try {
            Path targetPath = resolveAndValidatePath(storageKey);

            if (!Files.exists(targetPath)) {
                throw new BusinessException(ErrorCode.STORAGE_OBJECT_NOT_FOUND,
                    "存储对象不存在: " + storageKey);
            }

            return Files.size(targetPath);

        } catch (IOException e) {
            log.error("获取文件大小失败: storageKey={}", storageKey, e);
            throw new BusinessException(ErrorCode.STORAGE_READ_FAILED, "获取文件大小失败: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            Path targetPath = resolveAndValidatePath(storageKey);
            return Files.exists(targetPath);
        } catch (Exception e) {
            log.warn("检查文件存在性失败: storageKey={}", storageKey, e);
            return false;
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path targetPath = resolveAndValidatePath(storageKey);
            Files.deleteIfExists(targetPath);
            log.debug("文件删除成功: storageKey={}", storageKey);
        } catch (IOException e) {
            log.error("文件删除失败: storageKey={}", storageKey, e);
            throw new BusinessException(ErrorCode.STORAGE_WRITE_FAILED, "文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 解析并校验路径，防止目录穿越攻击
     */
    private Path resolveAndValidatePath(String storageKey) {
        Path targetPath = rootPath.resolve(storageKey).normalize();

        // 安全检查：确保解析后的路径仍在 rootPath 下
        if (!targetPath.startsWith(rootPath)) {
            throw new BusinessException(ErrorCode.STORAGE_WRITE_FAILED,
                "非法的存储路径: " + storageKey);
        }

        return targetPath;
    }
}
