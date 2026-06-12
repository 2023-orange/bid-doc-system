package com.example.biddoc.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;
import com.example.biddoc.project.constant.ProjectMemberRoleEnum;
import com.example.biddoc.project.entity.ProjectMemberEntity;
import com.example.biddoc.project.mapper.ProjectMemberMapper;
import com.example.biddoc.project.service.ProjectPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectPermissionServiceImpl implements ProjectPermissionService {

    private final ProjectMemberMapper projectMemberMapper;

    @Override
    public void checkView(Long projectId) {
        UserContext.UserInfo user = requireUser();
        if (user.isSuperAdmin() || isMember(projectId, user.getUserId(), null)) {
            return;
        }
        throw new BusinessException(ErrorCode.PROJECT_PERMISSION_DENIED);
    }

    @Override
    public void checkManage(Long projectId) {
        UserContext.UserInfo user = requireUser();
        if (user.isSuperAdmin() || isMember(projectId, user.getUserId(), ProjectMemberRoleEnum.OWNER.getCode())) {
            return;
        }
        throw new BusinessException(ErrorCode.PROJECT_PERMISSION_DENIED);
    }

    @Override
    public void checkManageOrOwner(Long projectId, Long checklistOwnerUserId) {
        UserContext.UserInfo user = requireUser();
        if (Objects.equals(user.getUserId(), checklistOwnerUserId)) {
            return;
        }
        checkManage(projectId);
    }

    private boolean isMember(Long projectId, Long userId, String role) {
        LambdaQueryWrapper<ProjectMemberEntity> wrapper = new LambdaQueryWrapper<ProjectMemberEntity>()
                .eq(ProjectMemberEntity::getProjectId, projectId)
                .eq(ProjectMemberEntity::getUserId, userId)
                .eq(ProjectMemberEntity::getDeleted, false);
        if (role != null) {
            wrapper.eq(ProjectMemberEntity::getMemberRole, role);
        }
        return projectMemberMapper.selectCount(wrapper) > 0;
    }

    private UserContext.UserInfo requireUser() {
        UserContext.UserInfo user = UserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        return user;
    }
}
