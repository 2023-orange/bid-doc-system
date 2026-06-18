package com.example.biddoc.workflow.service;

import com.example.biddoc.common.result.PageResponse;
import com.example.biddoc.workflow.dto.req.ApprovalDefinitionSaveReqDTO;
import com.example.biddoc.workflow.dto.req.ApprovalNodeSaveReqDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalDefinitionRespDTO;
import com.example.biddoc.workflow.dto.resp.ApprovalNodeRespDTO;

public interface ApprovalDefinitionService {

    Long create(ApprovalDefinitionSaveReqDTO req);

    void update(Long id, ApprovalDefinitionSaveReqDTO req);

    ApprovalDefinitionRespDTO get(Long id);

    PageResponse<ApprovalDefinitionRespDTO> list(String scenario, String bizType, Boolean enabled, Integer page, Integer size);

    void enable(Long id);

    void disable(Long id);

    Long addNode(Long definitionId, ApprovalNodeSaveReqDTO req);

    void updateNode(Long definitionId, Long nodeId, ApprovalNodeSaveReqDTO req);

    void deleteNode(Long definitionId, Long nodeId);

    ApprovalNodeRespDTO getFirstNode(Long definitionId);
}
