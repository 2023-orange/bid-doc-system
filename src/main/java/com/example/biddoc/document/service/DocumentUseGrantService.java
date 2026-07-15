package com.example.biddoc.document.service;

import com.example.biddoc.document.dto.req.DocumentUseGrantCreateReqDTO;
import com.example.biddoc.document.dto.resp.DocumentUseGrantRespDTO;

import java.util.List;

public interface DocumentUseGrantService {

    Long createGrant(Long documentId, DocumentUseGrantCreateReqDTO req);

    void revokeGrant(Long documentId, Long grantId);

    List<DocumentUseGrantRespDTO> listGrants(Long documentId);
}
