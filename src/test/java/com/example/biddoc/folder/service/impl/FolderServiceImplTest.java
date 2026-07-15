package com.example.biddoc.folder.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.document.service.DocumentService;
import com.example.biddoc.folder.dto.req.FolderCopyReqDTO;
import com.example.biddoc.folder.dto.req.FolderMoveReqDTO;
import com.example.biddoc.folder.dto.resp.FolderActionResultRespDTO;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.mapper.FolderFavoriteMapper;
import com.example.biddoc.folder.mapper.FolderGrantMapper;
import com.example.biddoc.folder.mapper.FolderManagerMapper;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderInsightService;
import com.example.biddoc.folder.service.FolderPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FolderServiceImplTest {

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FolderEntity.class);
    }

    private final FolderMapper folderMapper = mock(FolderMapper.class);
    private final FolderGrantMapper folderGrantMapper = mock(FolderGrantMapper.class);
    private final FolderManagerMapper folderManagerMapper = mock(FolderManagerMapper.class);
    private final FolderFavoriteMapper folderFavoriteMapper = mock(FolderFavoriteMapper.class);
    private final FolderPermissionService folderPermissionService = mock(FolderPermissionService.class);
    private final FolderInsightService folderInsightService = mock(FolderInsightService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final DocumentService documentService = mock(DocumentService.class);

    private final FolderServiceImpl service = new FolderServiceImpl(
            folderMapper,
            folderGrantMapper,
            folderManagerMapper,
            folderFavoriteMapper,
            folderPermissionService,
            folderInsightService,
            auditService,
            documentService
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void moveUsesTargetEditPermissionAndReturnsActionResult() {
        UserContext.set(new UserContext.UserInfo(7L, "folderAdmin", List.of("FOLDER_ADMIN"), 100L));
        FolderEntity source = folder(100L, 10L, "技术资料", 2, "10,100");
        FolderEntity target = folder(200L, 20L, "目标目录", 1, "20,200");
        FolderActionResultRespDTO actionResult = actionResult(100L, "技术资料", 200L, "/目标目录/技术资料");

        when(folderMapper.selectOne(any())).thenReturn(source, target);
        doNothing().when(folderPermissionService).checkMove(source);
        doNothing().when(folderPermissionService).checkEdit(target);
        when(folderMapper.selectCount(any())).thenReturn(0L);
        when(folderMapper.selectSubtree(100L)).thenReturn(List.of(source));
        when(folderMapper.selectList(any())).thenReturn(List.of());
        when(folderInsightService.toActionResult(100L)).thenReturn(actionResult);

        FolderMoveReqDTO req = new FolderMoveReqDTO();
        req.setTargetParentId(200L);
        var result = service.move(100L, req);

        assertEquals("/目标目录/技术资料", result.getFullPath());
        verify(folderPermissionService).checkEdit(target);
        verify(folderPermissionService, never()).checkCreateChild(target);
    }

    @Test
    void copyUsesDefaultCopyNameWithSuffixAndDoesNotTouchDocuments() {
        UserContext.set(new UserContext.UserInfo(7L, "folderAdmin", List.of("FOLDER_ADMIN"), 100L));
        FolderEntity source = folder(100L, 10L, "技术资料", 2, "10,100");
        FolderEntity target = folder(200L, 20L, "目标目录", 1, "20,200");
        FolderActionResultRespDTO actionResult = actionResult(900L, "技术资料 - 副本 (2)", 200L,
                "/目标目录/技术资料 - 副本 (2)");

        when(folderMapper.selectOne(any())).thenReturn(source, target);
        doNothing().when(folderPermissionService).checkCopy(source);
        doNothing().when(folderPermissionService).checkEdit(target);
        when(folderMapper.selectCount(any())).thenReturn(1L, 0L);
        when(folderMapper.selectSubtree(100L)).thenReturn(List.of(source));
        when(folderMapper.selectList(any())).thenReturn(List.of());
        when(folderInsightService.toActionResult(anyLong())).thenReturn(actionResult);

        FolderCopyReqDTO req = new FolderCopyReqDTO();
        req.setTargetParentId(200L);
        var result = service.copy(100L, req);

        assertEquals("技术资料 - 副本 (2)", result.getName());
        verify(folderPermissionService).checkEdit(target);
        verify(folderPermissionService, never()).checkCreateChild(target);
        verify(folderMapper).insert(argThat(folder ->
                folder instanceof FolderEntity
                        && "技术资料 - 副本 (2)".equals(((FolderEntity) folder).getName())
                        && Long.valueOf(200L).equals(((FolderEntity) folder).getParentId())
        ));
        verifyNoInteractions(documentService);
    }

    private static FolderEntity folder(Long id, Long parentId, String name, Integer level, String ancestorIds) {
        FolderEntity folder = new FolderEntity();
        folder.setId(id);
        folder.setParentId(parentId);
        folder.setName(name);
        folder.setLevel(level);
        folder.setSortNo(1);
        folder.setAncestorIds(ancestorIds);
        folder.setOwnerUserId(7L);
        folder.setOwnerDeptId(100L);
        folder.setInheritPermission(true);
        folder.setStatus(1);
        folder.setDeleted(false);
        return folder;
    }

    private static FolderActionResultRespDTO actionResult(Long id, String name, Long parentId, String fullPath) {
        FolderActionResultRespDTO result = new FolderActionResultRespDTO();
        result.setId(id);
        result.setName(name);
        result.setParentId(parentId);
        result.setFullPath(fullPath);
        return result;
    }
}
