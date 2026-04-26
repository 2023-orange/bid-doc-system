package com.example.biddoc.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.biddoc.audit.entity.AuditOperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditOperationLogMapper extends BaseMapper<AuditOperationLogEntity> {
}
