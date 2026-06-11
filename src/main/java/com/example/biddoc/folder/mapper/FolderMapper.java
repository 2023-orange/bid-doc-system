package com.example.biddoc.folder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.biddoc.folder.entity.FolderEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FolderMapper extends BaseMapper<FolderEntity> {

    /**
     * 查询以 folderId 为根的子树（含自身）。
     * ancestorIds 是逗号分隔字符串且"包含自身"，所以匹配条件是 folderId 出现在任一位置。
     * 用四段拼接而非单个 LIKE 避免边界误匹配（例如 id=10 匹配到 '1,210'）。
     * 排序保证 BFS 顺序：父先于子被处理，copy 中的 oldId→newId 映射方可正常构建。
     */
    @Select("SELECT * FROM doc_folder " +
            "WHERE deleted = false AND (" +
            "ancestor_ids = CAST(#{folderId} AS VARCHAR) " +
            "OR ancestor_ids LIKE CONCAT(CAST(#{folderId} AS VARCHAR), ',%') " +
            "OR ancestor_ids LIKE CONCAT('%,', CAST(#{folderId} AS VARCHAR), ',%') " +
            "OR ancestor_ids LIKE CONCAT('%,', CAST(#{folderId} AS VARCHAR))" +
            ") ORDER BY level ASC, sort_no ASC")
    List<FolderEntity> selectSubtree(@Param("folderId") Long folderId);

    /**
     * 批量逻辑删除指定 id 的 folder 行（move/delete 用）。
     * 不依赖 MyBatis-Plus 自动填充（@Update 注解不走 MetaObjectHandler），手动写 updated_at / updated_by。
     */
    @Update("<script>" +
            "UPDATE doc_folder SET deleted = true, updated_at = now(), updated_by = #{operator} " +
            "WHERE deleted = false AND id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int batchSoftDeleteByIds(@Param("ids") List<Long> ids, @Param("operator") String operator);

    /**
     * 子树 ancestorIds 前缀替换 + level 整体偏移（move 用）。
     * oldPrefix = 源节点 move 前的 ancestorIds（不含逗号后缀）；
     * newPrefix = 源节点 move 后的 ancestorIds；
     * 每个后代的 ancestor_ids 以 oldPrefix 开头，替换后变为 newPrefix + 原后缀（含开头的","）。
     * 用 SUBSTRING 切走 oldPrefix 部分后 CONCAT 新前缀，比 REPLACE 安全（避免 oldPrefix 在后代 ancestor_ids 中部出现的误替换）。
     * <p>
     * #{newPrefix} 必须显式 CAST AS VARCHAR：因 application.yml 配置了 stringtype=unspecified（为支持 jsonb 写入），
     * JDBC 不再为 String 参数预声明 varchar；而 CONCAT(?, ...) 第一参数没有列上下文给 PG 推断类型，
     * 不显式 CAST 会触发 "could not determine data type of parameter $1"。
     */
    @Update("<script>" +
            "UPDATE doc_folder SET " +
            "ancestor_ids = CONCAT(CAST(#{newPrefix} AS VARCHAR), SUBSTRING(ancestor_ids FROM #{oldPrefixLength} + 1)), " +
            "level = level + #{delta}, " +
            "updated_at = now(), updated_by = #{operator} " +
            "WHERE deleted = false AND id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int rewriteSubtreeAncestors(@Param("ids") List<Long> ids,
                                @Param("newPrefix") String newPrefix,
                                @Param("oldPrefixLength") int oldPrefixLength,
                                @Param("delta") int delta,
                                @Param("operator") String operator);
}
