package com.example.biddoc.document.storage;

import java.io.InputStream;

/**
 * 存储适配器接口，抽象文件存储操作。
 * MVP 实现：LocalDiskStorageAdapter
 * Goal-2 实现：MinioStorageAdapter
 */
public interface StorageAdapter {

    /**
     * 写入一个对象，返回 storageKey。
     *
     * @param in            输入流（调用方负责关闭）
     * @param size          预期字节数（用于 MinIO 的 putObject；LocalDisk 也用来防御性校验）
     * @param contentType   服务端探测的 MIME，可空
     * @param hintExt       原文件扩展名（含点，例如 ".docx"），可空；LocalDisk 实现可用于命名，MinIO 实现忽略
     * @return              storageKey，稳定不变，供后续 get/delete 使用
     */
    String put(InputStream in, long size, String contentType, String hintExt);

    /**
     * 读取一个对象，返回输入流（调用方负责关闭）。
     *
     * @param storageKey 存储键
     * @return 输入流
     */
    InputStream get(String storageKey);

    /**
     * 返回对象字节数。LocalDisk 通过 Files.size；MinIO 通过 statObject。
     *
     * @param storageKey 存储键
     * @return 字节数
     */
    long size(String storageKey);

    /**
     * 判断对象是否存在。
     *
     * @param storageKey 存储键
     * @return true 表示存在
     */
    boolean exists(String storageKey);

    /**
     * 物理删除对象。MVP 不调用，留接口给后续清理任务用。
     *
     * @param storageKey 存储键
     */
    void delete(String storageKey);
}
