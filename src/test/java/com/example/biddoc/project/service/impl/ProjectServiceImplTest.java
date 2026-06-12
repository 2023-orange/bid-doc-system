package com.example.biddoc.project.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.auth.mapper.SysDepartmentMapper;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.notify.service.NotificationService;
import com.example.biddoc.project.dto.req.ProjectCreateReqDTO;
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.entity.ProjectMemberEntity;
import com.example.biddoc.project.entity.ProjectNoSequenceEntity;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.project.mapper.ProjectNoSequenceMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectServiceImplTest {

    private final ProjectMapper projectMapper = mock(ProjectMapper.class);
    private final ProjectMemberMapper projectMemberMapper = mock(ProjectMemberMapper.class);
    private final ProjectNoSequenceMapper sequenceMapper = mock(ProjectNoSequenceMapper.class);
    private final SysDepartmentMapper departmentMapper = mock(SysDepartmentMapper.class);
    private final AuditService auditService = mock(AuditService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ProjectServiceImpl service = new ProjectServiceImpl(
            projectMapper,
            projectMemberMapper,
            sequenceMapper,
            departmentMapper,
            auditService,
            notificationService
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createGeneratesProjectNoAndNotifiesMembers() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));
        SysDepartment dept = new SysDepartment();
        dept.setId(10L);
        dept.setExtensionData(Map.of("abbr", "GB"));

        when(departmentMapper.selectById(10L)).thenReturn(dept);
        when(sequenceMapper.selectOne(any())).thenReturn(null);

        ProjectCreateReqDTO req = new ProjectCreateReqDTO();
        req.setProjectName("国网投标项目");
        req.setTenderUnit("国网");
        req.setOwnerDeptId(10L);
        req.setProjectType("POWER");
        req.setBidDeadline(OffsetDateTime.parse("2026-07-01T10:00:00+08:00"));
        req.setOwnerUserIds(List.of(2L));
        req.setMemberUserIds(List.of(3L));

        Long id = service.create(req);

        verify(projectMapper).insert(argThat(project ->
                project.getId().equals(id)
                        && project.getProjectNo().startsWith("GB-")
                        && project.getProjectNo().endsWith("-001")
                        && "NORMAL".equals(project.getProjectStatus())
        ));
        verify(sequenceMapper).insert(any(ProjectNoSequenceEntity.class));
        verify(projectMemberMapper).insert(argThat(member ->
                Long.valueOf(2L).equals(member.getUserId())
                        && "OWNER".equals(member.getMemberRole())
        ));
        verify(notificationService).send(2L, "PROJECT_MEMBER", "你已加入投标项目", "项目：国网投标项目", "PROJECT", id);
    }

    @Test
    void createRejectsProjectWithoutOwner() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 10L));

        ProjectCreateReqDTO req = new ProjectCreateReqDTO();
        req.setProjectName("无负责人项目");
        req.setOwnerDeptId(10L);
        req.setOwnerUserIds(List.of());

        assertThrows(BusinessException.class, () -> service.create(req));
    }

    @Test
    void listFiltersNonAdminToJoinedProjects() {
        UserContext.set(new UserContext.UserInfo(3L, "employee", List.of("EMPLOYEE"), 10L));

        Page<ProjectEntity> page = new Page<>(1, 20);
        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        project.setProjectName("参与项目");
        project.setProjectNo("GB-20260612-001");
        page.setRecords(List.of(project));
        page.setTotal(1);
        when(projectMapper.selectPage(any(Page.class), any())).thenReturn(page);

        var result = service.list(null, null, null, null, null, null, null, null, null, 1, 20);

        assertEquals(1, result.getTotal());
        assertEquals("参与项目", result.getList().get(0).getProjectName());
    }
}
