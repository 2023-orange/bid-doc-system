package com.example.biddoc.folder.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 复制文件夹请求。
 * targetName 为空时新根节点使用“原名称 - 副本”；重名由服务层自动追加序号。
 * Copy 始终复制整子树（含所有后代），但不复制 grant/manager/favorite。
 */
@Data
public class FolderCopyReqDTO {

    @NotNull(message = "targetParentId不能为空")
    private Long targetParentId;

    @Size(max = 128, message = "targetName不能超过128个字符")
    private String targetName;
}
