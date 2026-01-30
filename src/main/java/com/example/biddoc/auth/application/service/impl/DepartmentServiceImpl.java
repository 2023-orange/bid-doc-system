package com.example.biddoc.auth.application.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.auth.application.service.DepartmentService;
import com.example.biddoc.auth.domain.enity.SysDepartment;
import com.example.biddoc.auth.infrastructure.mapper.SysDepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final SysDepartmentMapper mapper;

    @Override
    public List<SysDepartment> listAll() {
        return mapper.selectList(
                Wrappers.<SysDepartment>lambdaQuery()
                        .eq(SysDepartment::getDeleted, false)
        );
    }

    @Override
    public void create(SysDepartment department) {
        mapper.insert(department);
    }
}
