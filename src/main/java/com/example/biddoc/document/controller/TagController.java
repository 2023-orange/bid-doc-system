package com.example.biddoc.document.controller;

import com.example.biddoc.common.result.ApiResponse;
import com.example.biddoc.document.dto.req.DocumentTagBindReqDTO;
import com.example.biddoc.document.dto.req.TagCreateReqDTO;
import com.example.biddoc.document.dto.resp.TagRespDTO;
import com.example.biddoc.document.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("/tags")
    public ApiResponse<List<TagRespDTO>> list() {
        return ApiResponse.success(tagService.list());
    }

    @PostMapping("/tags")
    public ApiResponse<TagRespDTO> create(@Valid @RequestBody TagCreateReqDTO req) {
        return ApiResponse.success(tagService.create(req.getName()));
    }

    @DeleteMapping("/tags/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ApiResponse.success();
    }

    @PutMapping("/documents/{id}/tags")
    public ApiResponse<Void> bindDocumentTags(@PathVariable Long id,
                                              @RequestBody DocumentTagBindReqDTO req) {
        tagService.bindDocumentTags(id, req != null ? req.getTagIds() : null);
        return ApiResponse.success();
    }

    @GetMapping("/documents/{id}/tags")
    public ApiResponse<List<TagRespDTO>> listDocumentTags(@PathVariable Long id) {
        return ApiResponse.success(tagService.listDocumentTags(id));
    }
}
