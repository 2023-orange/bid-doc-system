package com.example.biddoc.folder.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移动文件夹请求。
 * targetParentId 必须为非根级目录的 id；MVP 不允许 move 到根级，
 * 因此 targetParentId = 0 在 Service 层会被拒绝（FOLDER_MOVE_TARGET_INVALID）。
 */
@Data
public class FolderMoveReqDTO {

    @NotNull(message = "targetParentId不能为空")
    private Long targetParentId;
}
