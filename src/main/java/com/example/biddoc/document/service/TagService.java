package com.example.biddoc.document.service;

import com.example.biddoc.document.dto.resp.TagRespDTO;

import java.util.List;

public interface TagService {

    List<TagRespDTO> list();

    TagRespDTO create(String name);

    void delete(Long id);

    void bindDocumentTags(Long documentId, List<Long> tagIds);

    List<TagRespDTO> listDocumentTags(Long documentId);
}
