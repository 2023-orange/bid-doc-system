package com.example.biddoc.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.biddoc.document.entity.DownloadLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 下载日志 Mapper
 */
@Mapper
public interface DownloadLogMapper extends BaseMapper<DownloadLogEntity> {
}
