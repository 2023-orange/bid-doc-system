package com.example.biddoc.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.biddoc.document.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文档 Mapper
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity> {

    /**
     * 批量软删除指定 folder ID 集合下的所有文档
     * 由 FolderServiceImpl.doBatchDelete 在级联删除时调用
     *
     * @param folderIds folder ID 集合
     * @param updatedBy 更新人
     * @return 受影响的行数
     */
    @Update("<script>" +
            "UPDATE doc_document " +
            "SET deleted = true, " +
            "    updated_at = now(), " +
            "    updated_by = #{updatedBy} " +
            "WHERE deleted = false " +
            "  AND folder_id IN " +
            "  <foreach collection='folderIds' item='folderId' open='(' separator=',' close=')'>" +
            "    #{folderId}" +
            "  </foreach>" +
            "</script>")
    int batchSoftDeleteByFolderIds(@Param("folderIds") List<Long> folderIds,
                                    @Param("updatedBy") String updatedBy);
}
