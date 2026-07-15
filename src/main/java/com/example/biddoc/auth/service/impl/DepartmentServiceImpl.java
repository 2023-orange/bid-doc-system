package com.example.biddoc.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.biddoc.auth.dto.req.DepartmentStatusUpdateReqDTO;
import com.example.biddoc.auth.dto.req.DepartmentUpdateReqDTO;
import com.example.biddoc.auth.dto.resp.DepartmentTreeRespDTO;
import com.example.biddoc.auth.entity.SysDepartment;
import com.example.biddoc.auth.entity.SysUser;
import com.example.biddoc.auth.mapper.SysDepartmentMapper;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.auth.service.DepartmentService;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.project.constant.ProjectStatusEnum;
import com.example.biddoc.project.entity.ProjectEntity;
import com.example.biddoc.project.mapper.ProjectMapper;
import com.example.biddoc.workflow.entity.ApprovalDefinitionEntity;
import com.example.biddoc.workflow.mapper.ApprovalDefinitionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private static final int MAX_LEVEL = 5;
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    private final SysDepartmentMapper mapper;
    private final SysUserMapper userMapper;
    private final FolderMapper folderMapper;
    private final DocumentMapper documentMapper;
    private final ProjectMapper projectMapper;
    private final ApprovalDefinitionMapper approvalDefinitionMapper;

    @Override
    public List<SysDepartment> listAll() {
        return mapper.selectList(
                Wrappers.<SysDepartment>lambdaQuery()
                        .eq(SysDepartment::getDeleted, false)
                        .orderByAsc(SysDepartment::getLevel)
                        .orderByAsc(SysDepartment::getSortOrder)
                        .orderByAsc(SysDepartment::getId)
        );
    }

    @Override
    public List<DepartmentTreeRespDTO> listTree() {
        List<SysDepartment> departments = sortedDepartments(listAll());
        Map<Long, SysDepartment> entityMap = departments.stream()
                .collect(Collectors.toMap(SysDepartment::getId, Function.identity(), (left, right) -> left));
        Map<Long, DepartmentTreeRespDTO> nodeMap = departments.stream()
                .collect(Collectors.toMap(SysDepartment::getId, department -> toTreeNode(department, entityMap), (left, right) -> left));

        List<DepartmentTreeRespDTO> roots = new ArrayList<>();
        for (SysDepartment department : departments) {
            DepartmentTreeRespDTO node = nodeMap.get(department.getId());
            DepartmentTreeRespDTO parent = nodeMap.get(normalizeParentId(department.getParentId()));
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        sortAndMarkTree(roots);
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SysDepartment department) {
        Long parentId = normalizeParentId(department.getParentId());
        SysDepartment parent = parentId == null ? null : getActiveDepartment(parentId);
        int level = parent == null ? 1 : parent.getLevel() + 1;
        assertLevelAllowed(level);
        if (department.getStatus() != null) {
            assertStatusAllowed(department.getStatus());
        }

        department.setParentId(parentId);
        department.setLevel(level);
        department.setSortOrder(defaultSortOrder(department.getSortOrder()));
        department.setStatus(defaultStatus(department.getStatus()));
        department.setDeleted(false);
        mapper.insert(department);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DepartmentUpdateReqDTO req) {
        SysDepartment current = getActiveDepartment(id);
        List<SysDepartment> departments = listAll();
        Map<Long, SysDepartment> entityMap = departments.stream()
                .collect(Collectors.toMap(SysDepartment::getId, Function.identity(), (left, right) -> left));

        Long targetParentId = normalizeParentId(req.getParentId());
        if (Objects.equals(id, targetParentId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "部门不能移动到自身下");
        }

        SysDepartment targetParent = null;
        if (targetParentId != null) {
            targetParent = entityMap.get(targetParentId);
            if (targetParent == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "父级部门不存在");
            }
        }

        List<SysDepartment> descendants = findDescendants(id, departments);
        Set<Long> descendantIds = descendants.stream().map(SysDepartment::getId).collect(Collectors.toSet());
        if (targetParentId != null && descendantIds.contains(targetParentId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "部门不能移动到自己的下级部门下");
        }

        int oldLevel = current.getLevel();
        int newLevel = targetParent == null ? 1 : targetParent.getLevel() + 1;
        assertLevelAllowed(newLevel + maxDepthBelowCurrent(descendants, oldLevel));
        if (req.getStatus() != null) {
            assertStatusAllowed(req.getStatus());
        }

        current.setName(req.getName());
        current.setParentId(targetParentId);
        current.setLevel(newLevel);
        current.setSortOrder(defaultSortOrder(req.getSortOrder()));
        current.setManagerUserId(req.getManagerUserId());
        current.setStatus(req.getStatus() == null ? current.getStatus() : req.getStatus());
        current.setRemark(req.getRemark());

        // 移动部门会改变整棵子树的层级，必须和当前节点更新放在同一个事务内保持一致。
        int delta = newLevel - oldLevel;
        mapper.updateById(current);
        if (delta != 0) {
            for (SysDepartment descendant : descendants) {
                descendant.setLevel(descendant.getLevel() + delta);
                mapper.updateById(descendant);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, DepartmentStatusUpdateReqDTO req) {
        assertStatusAllowed(req.getStatus());
        SysDepartment current = getActiveDepartment(id);
        List<SysDepartment> departments = listAll();
        Set<Long> descendantIds = findDescendants(id, departments).stream()
                .map(SysDepartment::getId)
                .collect(Collectors.toSet());

        List<Long> cascadeDeptIds = req.getCascadeDeptIds() == null ? List.of() : req.getCascadeDeptIds();
        if (!descendantIds.containsAll(cascadeDeptIds)) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "连带禁用部门必须属于当前部门的下级部门");
        }

        // 非叶子节点是否连带禁用由前端弹窗选择；后端只校验范围，空集合表示只处理当前部门。
        current.setStatus(req.getStatus());
        mapper.updateById(current);
        for (Long cascadeDeptId : new HashSet<>(cascadeDeptIds)) {
            SysDepartment cascade = getActiveDepartment(cascadeDeptId);
            cascade.setStatus(req.getStatus());
            mapper.updateById(cascade);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDepartment current = getActiveDepartment(id);
        List<SysDepartment> departments = listAll();
        if (!findDescendants(id, departments).isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "存在下级部门，不能删除");
        }
        assertNoBusinessReference(id);

        current.setDeleted(true);
        mapper.updateById(current);
    }

    private SysDepartment getActiveDepartment(Long id) {
        SysDepartment department = mapper.selectById(id);
        if (department == null || Boolean.TRUE.equals(department.getDeleted())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "部门不存在");
        }
        return department;
    }

    private void assertNoBusinessReference(Long id) {
        // 部门 ID 已被多处业务数据引用时只能禁用，不能删除，避免历史数据和权限配置失去归属。
        if (userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getDeleted, false)
                .eq(SysUser::getDeptId, id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "部门下存在用户，不能删除");
        }
        if (folderMapper.selectCount(Wrappers.<FolderEntity>lambdaQuery()
                .eq(FolderEntity::getDeleted, false)
                .eq(FolderEntity::getOwnerDeptId, id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "部门已被文件夹引用，不能删除");
        }
        if (documentMapper.selectCount(Wrappers.<DocumentEntity>lambdaQuery()
                .eq(DocumentEntity::getDeleted, false)
                .eq(DocumentEntity::getOwnerDeptId, id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "部门已被资料引用，不能删除");
        }
        if (projectMapper.selectCount(Wrappers.<ProjectEntity>lambdaQuery()
                .eq(ProjectEntity::getDeleted, false)
                .eq(ProjectEntity::getOwnerDeptId, id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "部门已被项目引用，不能删除");
        }
        if (approvalDefinitionMapper.selectCount(Wrappers.<ApprovalDefinitionEntity>lambdaQuery()
                .eq(ApprovalDefinitionEntity::getDeleted, false)
                .eq(ApprovalDefinitionEntity::getDeptId, id)) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "部门已被审批流程引用，不能删除");
        }
    }

    private DepartmentTreeRespDTO toTreeNode(SysDepartment department, Map<Long, SysDepartment> entityMap) {
        DepartmentTreeRespDTO node = new DepartmentTreeRespDTO();
        node.setId(department.getId());
        node.setName(department.getName());
        node.setParentId(normalizeParentId(department.getParentId()));
        SysDepartment parent = entityMap.get(node.getParentId());
        node.setParentName(parent == null ? null : parent.getName());
        node.setLevel(department.getLevel());
        node.setSortOrder(defaultSortOrder(department.getSortOrder()));
        node.setManagerUserId(department.getManagerUserId());
        node.setStatus(defaultStatus(department.getStatus()));
        node.setRemark(department.getRemark());
        node.setCreatedAt(department.getCreatedAt());
        node.setUpdatedAt(department.getUpdatedAt());
        node.setHasChildren(false);
        node.setMemberCount(countDirectEnabledMembers(department.getId()));
        node.setProjectCount(countActiveProjects(department.getId()));
        node.setChildren(new ArrayList<>());
        return node;
    }

    private Integer countDirectEnabledMembers(Long departmentId) {
        // 部门人数只统计直属且启用的用户，不向下汇总子部门，避免父级部门数字和组织树层级语义混淆。
        return countToInteger(userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getDeleted, false)
                .eq(SysUser::getDeptId, departmentId)
                .eq(SysUser::getStatus, STATUS_ENABLED)));
    }

    private Integer countActiveProjects(Long departmentId) {
        // 前端“进行中项目数”口径：当前项目状态共有 NORMAL、PAUSED、ARCHIVED、CANCELLED、DELETED 五种；
        // 只有 NORMAL 且未软删除的项目计入，PAUSED/ARCHIVED/CANCELLED/DELETED 都不计入。
        return countToInteger(projectMapper.selectCount(Wrappers.<ProjectEntity>lambdaQuery()
                .eq(ProjectEntity::getDeleted, false)
                .eq(ProjectEntity::getOwnerDeptId, departmentId)
                .eq(ProjectEntity::getProjectStatus, ProjectStatusEnum.NORMAL.getCode())));
    }

    private Integer countToInteger(Long count) {
        return count == null ? 0 : Math.toIntExact(count);
    }

    private void sortAndMarkTree(List<DepartmentTreeRespDTO> nodes) {
        nodes.sort(Comparator.comparing(DepartmentTreeRespDTO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(DepartmentTreeRespDTO::getId, Comparator.nullsLast(Long::compareTo)));
        for (DepartmentTreeRespDTO node : nodes) {
            sortAndMarkTree(node.getChildren());
            node.setHasChildren(!node.getChildren().isEmpty());
        }
    }

    private List<SysDepartment> sortedDepartments(List<SysDepartment> departments) {
        return departments.stream()
                .sorted(Comparator.comparing(SysDepartment::getLevel, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(department -> defaultSortOrder(department.getSortOrder()))
                        .thenComparing(SysDepartment::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    private List<SysDepartment> findDescendants(Long id, List<SysDepartment> departments) {
        Map<Long, List<SysDepartment>> childrenMap = departments.stream()
                .filter(department -> normalizeParentId(department.getParentId()) != null)
                .collect(Collectors.groupingBy(department -> normalizeParentId(department.getParentId())));
        List<SysDepartment> descendants = new ArrayList<>();
        collectDescendants(id, childrenMap, descendants);
        return descendants;
    }

    private void collectDescendants(Long id, Map<Long, List<SysDepartment>> childrenMap, List<SysDepartment> descendants) {
        for (SysDepartment child : childrenMap.getOrDefault(id, List.of())) {
            descendants.add(child);
            collectDescendants(child.getId(), childrenMap, descendants);
        }
    }

    private int maxDepthBelowCurrent(List<SysDepartment> descendants, int oldLevel) {
        return descendants.stream()
                .map(SysDepartment::getLevel)
                .filter(Objects::nonNull)
                .mapToInt(level -> level - oldLevel)
                .max()
                .orElse(0);
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || Objects.equals(0L, parentId) ? null : parentId;
    }

    private Integer defaultSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    private Integer defaultStatus(Integer status) {
        return status == null ? STATUS_ENABLED : status;
    }

    private void assertLevelAllowed(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new BusinessException(ErrorCode.BUSINESS_ILLEGAL, "部门层级不能超过5级");
        }
    }

    private void assertStatusAllowed(Integer status) {
        if (!Integer.valueOf(STATUS_DISABLED).equals(status) && !Integer.valueOf(STATUS_ENABLED).equals(status)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "部门状态只能是启用或禁用");
        }
    }
}
