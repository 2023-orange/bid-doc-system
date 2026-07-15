package com.example.biddoc.folder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.biddoc.auth.mapper.SysDepartmentMapper;
import com.example.biddoc.auth.mapper.SysUserMapper;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.document.entity.DocumentEntity;
import com.example.biddoc.document.entity.TagEntity;
import com.example.biddoc.document.mapper.DocumentMapper;
import com.example.biddoc.document.mapper.DownloadLogMapper;
import com.example.biddoc.document.mapper.TagMapper;
import com.example.biddoc.folder.dto.req.FolderAccessRecordReqDTO;
import com.example.biddoc.folder.dto.req.FolderTagBindReqDTO;
import com.example.biddoc.folder.entity.FolderAccessLogEntity;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.entity.FolderTagEntity;
import com.example.biddoc.folder.mapper.FolderAccessLogMapper;
import com.example.biddoc.folder.mapper.FolderFavoriteMapper;
import com.example.biddoc.folder.mapper.FolderGrantMapper;
import com.example.biddoc.folder.mapper.FolderManagerMapper;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.mapper.FolderTagMapper;
import com.example.biddoc.folder.service.FolderPermissionService;
import com.example.biddoc.project.mapper.ProjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FolderInsightServiceImplTest {

    private final FolderMapper folderMapper = mock(FolderMapper.class);
    private final DocumentMapper documentMapper = mock(DocumentMapper.class);
    private final DownloadLogMapper downloadLogMapper = mock(DownloadLogMapper.class);
    private final FolderFavoriteMapper folderFavoriteMapper = mock(FolderFavoriteMapper.class);
    private final FolderManagerMapper folderManagerMapper = mock(FolderManagerMapper.class);
    private final FolderGrantMapper folderGrantMapper = mock(FolderGrantMapper.class);
    private final FolderAccessLogMapper folderAccessLogMapper = mock(FolderAccessLogMapper.class);
    private final FolderTagMapper folderTagMapper = mock(FolderTagMapper.class);
    private final TagMapper tagMapper = mock(TagMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final SysDepartmentMapper sysDepartmentMapper = mock(SysDepartmentMapper.class);
    private final ProjectMapper projectMapper = mock(ProjectMapper.class);
    private final FolderPermissionService folderPermissionService = mock(FolderPermissionService.class);

    private final FolderInsightServiceImpl service = new FolderInsightServiceImpl(
            folderMapper,
            documentMapper,
            downloadLogMapper,
            folderFavoriteMapper,
            folderManagerMapper,
            folderGrantMapper,
            folderAccessLogMapper,
            folderTagMapper,
            tagMapper,
            sysUserMapper,
            sysDepartmentMapper,
            projectMapper,
            folderPermissionService
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void recordAccessWritesViewLogAfterViewPermissionCheck() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));
        FolderEntity folder = folder(10L, "项目资料");
        when(folderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);

        FolderAccessRecordReqDTO req = new FolderAccessRecordReqDTO();
        req.setAccessType("VIEW");
        service.recordAccess(10L, req);

        verify(folderPermissionService).checkView(folder);
        verify(folderAccessLogMapper).insert(argThat(log ->
                log instanceof FolderAccessLogEntity
                        && Long.valueOf(10L).equals(((FolderAccessLogEntity) log).getFolderId())
                        && Long.valueOf(7L).equals(((FolderAccessLogEntity) log).getUserId())
                        && "VIEW".equals(((FolderAccessLogEntity) log).getAccessType())
                        && ((FolderAccessLogEntity) log).getAccessTime() != null
                        && Boolean.FALSE.equals(((FolderAccessLogEntity) log).getDeleted())
        ));
    }

    @Test
    void bindFolderTagsReplacesRelationsAfterEditPermissionCheck() {
        UserContext.set(new UserContext.UserInfo(7L, "folderAdmin", List.of("FOLDER_ADMIN"), 100L));
        FolderEntity folder = folder(10L, "项目资料");
        when(folderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(folder);
        doNothing().when(folderPermissionService).checkEdit(folder);

        TagEntity important = tag(1L, "重要");
        TagEntity annual = tag(2L, "2026年度");
        when(tagMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(important, annual));

        FolderTagBindReqDTO req = new FolderTagBindReqDTO();
        req.setTagIds(List.of(1L, 2L, 2L));
        service.bindTags(10L, req);

        verify(folderPermissionService).checkEdit(folder);
        verify(folderTagMapper).delete(any(LambdaQueryWrapper.class));
        verify(folderTagMapper).insert(argThat(relation ->
                relation instanceof FolderTagEntity
                        && Long.valueOf(10L).equals(((FolderTagEntity) relation).getFolderId())
                        && Long.valueOf(1L).equals(((FolderTagEntity) relation).getTagId())
                        && Boolean.FALSE.equals(((FolderTagEntity) relation).getDeleted())
        ));
        verify(folderTagMapper).insert(argThat(relation ->
                relation instanceof FolderTagEntity
                        && Long.valueOf(10L).equals(((FolderTagEntity) relation).getFolderId())
                        && Long.valueOf(2L).equals(((FolderTagEntity) relation).getTagId())
                        && Boolean.FALSE.equals(((FolderTagEntity) relation).getDeleted())
        ));
    }

    @Test
    void getDetailMasksInvisibleAncestorsAndFiltersInvisibleDescendantStats() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        FolderEntity root = folder(10L, "根目录");
        root.setAncestorIds("10");
        FolderEntity hiddenParent = folder(11L, "隐藏父目录");
        hiddenParent.setParentId(10L);
        hiddenParent.setAncestorIds("10,11");
        hiddenParent.setLevel(1);
        FolderEntity current = folder(12L, "可见目录");
        current.setParentId(11L);
        current.setAncestorIds("10,11,12");
        current.setLevel(2);
        FolderEntity visibleChild = folder(13L, "可见子目录");
        visibleChild.setParentId(12L);
        visibleChild.setAncestorIds("10,11,12,13");
        visibleChild.setLevel(3);
        FolderEntity invisibleChild = folder(14L, "不可见子目录");
        invisibleChild.setParentId(12L);
        invisibleChild.setAncestorIds("10,11,12,14");
        invisibleChild.setLevel(3);

        DocumentEntity directDoc = document(100L, 12L, 100L);
        DocumentEntity visibleChildDoc = document(101L, 13L, 200L);

        when(folderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        doNothing().when(folderPermissionService).checkView(current);
        when(folderMapper.selectBatchIds(List.of(10L, 11L, 12L))).thenReturn(List.of(root, hiddenParent, current));
        when(folderMapper.selectById(11L)).thenReturn(hiddenParent);
        when(folderPermissionService.canView(root)).thenReturn(false);
        when(folderPermissionService.canView(hiddenParent)).thenReturn(false);
        when(folderPermissionService.canView(current)).thenReturn(true);
        when(folderPermissionService.canView(visibleChild)).thenReturn(true);
        when(folderPermissionService.canView(invisibleChild)).thenReturn(false);
        when(folderMapper.selectSubtree(12L)).thenReturn(List.of(current, visibleChild, invisibleChild));
        when(documentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(directDoc))
                .thenReturn(List.of(directDoc, visibleChildDoc));
        when(downloadLogMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(folderAccessLogMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(folderAccessLogMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(folderFavoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(folderTagMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        var detail = service.getDetail(12L);

        assertEquals("/.../可见目录", detail.getFullPath());
        assertEquals(1L, detail.getDocumentCount());
        assertEquals(2L, detail.getTotalDocumentCount());
        assertEquals(100L, detail.getFolderSize());
        assertEquals(300L, detail.getTotalSize());
        assertEquals(1L, detail.getChildFolderCount());
        assertEquals(1L, detail.getTotalChildFolderCount());
    }

    private static FolderEntity folder(Long id, String name) {
        FolderEntity folder = new FolderEntity();
        folder.setId(id);
        folder.setParentId(0L);
        folder.setName(name);
        folder.setAncestorIds(String.valueOf(id));
        folder.setLevel(0);
        folder.setDeleted(false);
        return folder;
    }

    private static TagEntity tag(Long id, String name) {
        TagEntity tag = new TagEntity();
        tag.setId(id);
        tag.setName(name);
        tag.setDeleted(false);
        return tag;
    }

    private static DocumentEntity document(Long id, Long folderId, Long size) {
        DocumentEntity document = new DocumentEntity();
        document.setId(id);
        document.setFolderId(folderId);
        document.setLatestSize(size);
        document.setDeleted(false);
        return document;
    }
}
