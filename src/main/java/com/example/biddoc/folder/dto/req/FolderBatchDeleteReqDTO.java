package com.example.biddoc.folder.dto.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除文件夹请求。
 * Service 层会做"父子去重"：若集合内某 id 的祖先也在集合中，则该 id 视为已被祖先覆盖，
 * 不再单独处理；最终对所有保留节点的整个子树做级联逻辑删除。
 */
@Data
public class FolderBatchDeleteReqDTO {

    @NotEmpty(message = "folderIds不能为空")
    private List<Long> folderIds;
}
