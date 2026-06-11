package com.example.biddoc.workflow.service;

import com.example.biddoc.workflow.dto.resp.ApprovalHistoryRespDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalTaskRespDTO;

import java.util.List;

public interface ApprovalService {

    Long submit(Long documentId, String comment);

    List<ApprovalTaskRespDTO> listMyTasks(String status);

    void approve(Long taskId, String comment);

    void reject(Long taskId, String comment);

    List<ApprovalHistoryRespDTO> history(Long documentId);
}
