package com.example.biddoc.auth.service.impl;

import com.example.biddoc.auth.dto.req.DepartmentStatusUpdateReqDTO;
import com.example.biddoc.auth.dto.req.DepartmentUpdateReqDTO;
import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.auth.mapper.SysDepartmentMapper;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.workflow.mapper.ApprovalDefinitionMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DepartmentServiceImplTest {

    private final SysDepartmentMapper mapper = mock(SysDepartmentMapper.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final FolderMapper folderMapper = mock(FolderMapper.class);
    private final DocumentMapper documentMapper = mock(DocumentMapper.class);
    private final ProjectMapper projectMapper = mock(ProjectMapper.class);
    private final ApprovalDefinitionMapper approvalDefinitionMapper = mock(ApprovalDefinitionMapper.class);
    private final DepartmentServiceImpl service = new DepartmentServiceImpl(
            mapper,
            userMapper,
            folderMapper,
            documentMapper,
            projectMapper,
            approvalDefinitionMapper
    );

    @Test
    void listTreeReturnsManagementFieldsAndParentName() {
        OffsetDateTime now = OffsetDateTime.now();
        SysDepartment root = department(1L, "事业部", null, 1, 20);
        root.setStatus(1);
        root.setRemark("root remark");
        root.setCreatedAt(now);
        SysDepartment child = department(2L, "技术部", 1L, 2, 10);
        child.setStatus(0);
        child.setCreatedAt(now.plusMinutes(1));
        when(mapper.selectList(any())).thenReturn(List.of(child, root));

        var tree = service.listTree();

        assertEquals(1, tree.size());
        assertEquals(1L, tree.get(0).getId());
        assertEquals("事业部", tree.get(0).getName());
        assertEquals(20, tree.get(0).getSortOrder());
        assertEquals(1, tree.get(0).getStatus());
        assertEquals("root remark", tree.get(0).getRemark());
        assertEquals(now, tree.get(0).getCreatedAt());
        assertEquals(true, tree.get(0).getHasChildren());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals(2L, tree.get(0).getChildren().get(0).getId());
        assertEquals("技术部", tree.get(0).getChildren().get(0).getName());
        assertEquals("事业部", tree.get(0).getChildren().get(0).getParentName());
        assertEquals(false, tree.get(0).getChildren().get(0).getHasChildren());
    }

    @Test
    void listTreeReturnsDepartmentStats() {
        SysDepartment root = department(1L, "事业部", null, 1, 20);
        when(mapper.selectList(any())).thenReturn(List.of(root));
        when(userMapper.selectCount(any())).thenReturn(2L);
        when(projectMapper.selectCount(any())).thenReturn(3L);

        var tree = service.listTree();

        assertEquals(2, tree.get(0).getMemberCount());
        assertEquals(3, tree.get(0).getProjectCount());
    }

    @Test
    void updateRejectsMovingDepartmentUnderItsDescendant() {
        SysDepartment current = department(2L, "技术部", 1L, 2, 1);
        SysDepartment descendant = department(3L, "交付组", 2L, 3, 1);
        when(mapper.selectById(2L)).thenReturn(current);
        when(mapper.selectList(any())).thenReturn(List.of(current, descendant));

        DepartmentUpdateReqDTO req = new DepartmentUpdateReqDTO();
        req.setName("技术部");
        req.setParentId(3L);
        req.setSortOrder(1);

        assertThrows(BusinessException.class, () -> service.update(2L, req));
        verify(mapper, never()).updateById(any());
    }

    @Test
    void updatePreservesStatusWhenRequestOmitsStatus() {
        SysDepartment current = department(2L, "技术部", 1L, 2, 1);
        current.setStatus(0);
        SysDepartment parent = department(1L, "事业部", null, 1, 1);
        when(mapper.selectById(2L)).thenReturn(current);
        when(mapper.selectList(any())).thenReturn(List.of(parent, current));

        DepartmentUpdateReqDTO req = new DepartmentUpdateReqDTO();
        req.setName("技术部");
        req.setParentId(1L);
        req.setSortOrder(1);

        service.update(2L, req);

        var captor = forClass(SysDepartment.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
    }

    @Test
    void changeStatusRejectsCascadeDepartmentsOutsideDescendants() {
        SysDepartment current = department(2L, "技术部", 1L, 2, 1);
        SysDepartment child = department(3L, "交付组", 2L, 3, 1);
        SysDepartment other = department(4L, "财务部", 1L, 2, 1);
        when(mapper.selectById(2L)).thenReturn(current);
        when(mapper.selectList(any())).thenReturn(List.of(current, child, other));

        DepartmentStatusUpdateReqDTO req = new DepartmentStatusUpdateReqDTO();
        req.setStatus(0);
        req.setCascadeDeptIds(List.of(4L));

        assertThrows(BusinessException.class, () -> service.changeStatus(2L, req));
        verify(mapper, never()).updateById(any());
    }

    @Test
    void deleteRejectsReferencedDepartment() {
        SysDepartment current = department(2L, "技术部", 1L, 2, 1);
        when(mapper.selectById(2L)).thenReturn(current);
        when(mapper.selectList(any())).thenReturn(List.of(current));
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.delete(2L));

        verify(mapper, never()).updateById(any());
    }

    private SysDepartment department(Long id, String name, Long parentId, Integer level, Integer sortOrder) {
        SysDepartment department = new SysDepartment();
        department.setId(id);
        department.setName(name);
        department.setParentId(parentId);
        department.setLevel(level);
        department.setSortOrder(sortOrder);
        department.setDeleted(false);
        return department;
    }
}
