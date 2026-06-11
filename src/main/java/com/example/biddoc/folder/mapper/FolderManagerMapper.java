package com.example.biddoc.folder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.biddoc.folder.entity.FolderManagerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FolderManagerMapper extends BaseMapper<FolderManagerEntity> {

    /**
     * 按 folder_id 集合批量逻辑删除 manager（delete 级联用）。
     * 返回受影响行数，可用于审计 affectedManagers 字段。
     */
    @Update("<script>" +
            "UPDATE doc_folder_manager SET deleted = true, updated_at = now(), updated_by = #{operator} " +
            "WHERE deleted = false AND folder_id IN " +
            "<foreach collection='folderIds' item='fid' open='(' separator=',' close=')'>#{fid}</foreach>" +
            "</script>")
    int batchSoftDeleteByFolderIds(@Param("folderIds") List<Long> folderIds, @Param("operator") String operator);
}
