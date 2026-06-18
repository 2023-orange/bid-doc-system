package com.example.biddoc.workflow.service;

import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.workflow.entity.ApprovalDefinitionEntity;
import com.example.biddoc.workflow.entity.ApprovalInstanceEntity;
import com.example.biddoc.workflow.entity.ApprovalNodeEntity;
import com.example.biddoc.workflow.entity.ApprovalTaskEntity;

public interface ApprovalFlowEngineService {

    ApprovalDefinitionEntity matchDefinition(String scenario, String bizModule, String bizType,
                                             Long deptId, String businessCategory);

    ApprovalTaskEntity createFirstTask(ApprovalInstanceEntity instance, FolderEntity folder,
                                       Long projectId, UserContext.UserInfo submitter);

    ApprovalTaskEntity createNextTask(ApprovalInstanceEntity instance, ApprovalTaskEntity currentTask,
                                      FolderEntity folder, Long projectId, UserContext.UserInfo submitter);

    Long resolveApprover(ApprovalNodeEntity node, ApprovalInstanceEntity instance, FolderEntity folder,
                         Long projectId, UserContext.UserInfo submitter);
}
