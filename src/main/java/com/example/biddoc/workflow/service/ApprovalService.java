package com.example.biddoc.workflow.service;

import com.example.biddoc.workflow.dto.resp.ApprovalHistoryRespDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalHandleResultRespDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalTaskRespDTO;

import java.util.List;

public interface ApprovalService {

    Long submit(Long documentId, String comment);

    Long submitVersion(Long documentId, Integer versionNo, String comment);

    Long submitChecklistItem(Long projectId, Long itemId, String comment);

    List<ApprovalTaskRespDTO> listMyTasks(String status);

    ApprovalHandleResultRespDTO approve(Long taskId, String comment);

    ApprovalHandleResultRespDTO reject(Long taskId, String comment);

    void withdraw(Long instanceId, String comment);

    void transfer(Long taskId, Long targetUserId, String comment);

    void addSign(Long taskId, Long assigneeUserId, String comment);

    void terminate(Long instanceId, String reason);

    List<ApprovalHistoryRespDTO> history(Long documentId);

    List<ApprovalHistoryRespDTO> projectHistory(Long projectId);
}
