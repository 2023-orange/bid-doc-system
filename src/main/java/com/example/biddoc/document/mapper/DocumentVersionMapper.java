package com.example.biddoc.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.biddoc.document.entity.DocumentVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文档版本 Mapper
 */
@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersionEntity> {

    /**
     * 批量软删除指定 document ID 集合下的所有版本
     * 由 DocumentService 在级联删除时调用
     *
     * @param documentIds document ID 集合
     * @param updatedBy   更新人
     * @return 受影响的行数
     */
    @Update("<script>" +
            "UPDATE doc_document_version " +
            "SET deleted = true, " +
            "    updated_at = now(), " +
            "    updated_by = #{updatedBy} " +
            "WHERE deleted = false " +
            "  AND document_id IN " +
            "  <foreach collection='documentIds' item='documentId' open='(' separator=',' close=')'>" +
            "    #{documentId}" +
            "  </foreach>" +
            "</script>")
    int batchSoftDeleteByDocumentIds(@Param("documentIds") List<Long> documentIds,
                                      @Param("updatedBy") String updatedBy);
}
