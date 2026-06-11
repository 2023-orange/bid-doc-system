package com.example.biddoc.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.document.entity.DocumentTagEntity;
import com.example.biddoc.document.entity.TagEntity;
import com.example.biddoc.document.mapper.DocumentTagMapper;
import com.example.biddoc.document.mapper.TagMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagServiceImplTest {

    private final TagMapper tagMapper = mock(TagMapper.class);
    private final DocumentTagMapper documentTagMapper = mock(DocumentTagMapper.class);
    private final TagServiceImpl service = new TagServiceImpl(tagMapper, documentTagMapper);

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void createNormalizesNameAndCreatesTag() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 1L));
        when(tagMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        var result = service.create(" 投标 ");

        assertEquals("投标", result.getName());
        verify(tagMapper).insert(argThat(tag ->
                "投标".equals(tag.getName()) && Boolean.FALSE.equals(tag.getDeleted())
        ));
    }

    @Test
    void bindDocumentTagsReplacesExistingRelations() {
        UserContext.set(new UserContext.UserInfo(1L, "admin", List.of("SUPER_ADMIN"), 1L));
        TagEntity tag = new TagEntity();
        tag.setId(10L);
        tag.setName("投标");
        tag.setDeleted(false);
        when(tagMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(tag));

        service.bindDocumentTags(100L, List.of(10L));

        verify(documentTagMapper).delete(any(LambdaQueryWrapper.class));
        verify(documentTagMapper).insert(argThat(relation ->
                relation instanceof DocumentTagEntity
                        && Long.valueOf(100L).equals(((DocumentTagEntity) relation).getDocumentId())
                        && Long.valueOf(10L).equals(((DocumentTagEntity) relation).getTagId())
                        && Boolean.FALSE.equals(((DocumentTagEntity) relation).getDeleted())
        ));
    }
}
