package com.example.biddoc.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.biddoc.document.entity.SearchHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索历史 Mapper
 */
@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistoryEntity> {
}
