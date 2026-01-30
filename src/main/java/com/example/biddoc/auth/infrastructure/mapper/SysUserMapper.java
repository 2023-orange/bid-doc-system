package com.example.biddoc.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.biddoc.auth.domain.enity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
