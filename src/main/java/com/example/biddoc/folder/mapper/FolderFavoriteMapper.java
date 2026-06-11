package com.example.biddoc.folder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.biddoc.folder.entity.FolderFavoriteEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FolderFavoriteMapper extends BaseMapper<FolderFavoriteEntity> {

    /**
     * 按 folder_id 集合批量逻辑删除 favorite（delete 级联用）。
     * favorite 表无 updated_at / updated_by（只插不改的设计），但级联删除时仍需翻 deleted 标志。
     * 返回受影响行数，可用于审计 affectedFavorites 字段。
     */
    @Update("<script>" +
            "UPDATE doc_folder_favorite SET deleted = true " +
            "WHERE deleted = false AND folder_id IN " +
            "<foreach collection='folderIds' item='fid' open='(' separator=',' close=')'>#{fid}</foreach>" +
            "</script>")
    int batchSoftDeleteByFolderIds(@Param("folderIds") List<Long> folderIds);
}
