package com.example.biddoc.folder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.biddoc.audit.service.AuditService;
import com.example.biddoc.common.constant.UserContext;
import com.example.biddoc.folder.entity.FolderEntity;
import com.example.biddoc.folder.entity.FolderFavoriteEntity;
import com.example.biddoc.folder.mapper.FolderFavoriteMapper;
import com.example.biddoc.folder.mapper.FolderMapper;
import com.example.biddoc.folder.service.FolderPermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FolderFavoriteServiceImplTest {

    private final FolderMapper folderMapper = mock(FolderMapper.class);
    private final FolderFavoriteMapper folderFavoriteMapper = mock(FolderFavoriteMapper.class);
    private final FolderPermissionService folderPermissionService = mock(FolderPermissionService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final FolderFavoriteServiceImpl service = new FolderFavoriteServiceImpl(
            folderMapper,
            folderFavoriteMapper,
            folderPermissionService,
            auditService
    );

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void favoriteFolderCreatesFavoriteWhenMissing() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        FolderEntity folder = folder(10L, "项目资料");
        when(folderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);
        when(folderFavoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.favorite(10L);

        verify(folderFavoriteMapper).insert(argThat(entity ->
                Long.valueOf(10L).equals(entity.getFolderId())
                        && Long.valueOf(7L).equals(entity.getUserId())
                        && Boolean.FALSE.equals(entity.getDeleted())
        ));
        verify(auditService).record(argThat(command ->
                "FOLDER".equals(command.getModuleCode())
                        && "FAVORITE".equals(command.getOperationType())
                        && Long.valueOf(10L).equals(command.getBizId())
                        && "项目资料".equals(command.getObjectName())
                        && "employee 收藏了文件夹《项目资料》".equals(command.getActionSummary())
        ));
    }

    @Test
    void unfavoriteFolderWritesBusinessTraceAuditWhenFavoriteExists() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        FolderEntity folder = folder(10L, "项目资料");
        FolderFavoriteEntity existing = new FolderFavoriteEntity();
        existing.setId(99L);
        existing.setFolderId(10L);
        existing.setUserId(7L);
        existing.setDeleted(false);

        when(folderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);
        when(folderFavoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service.unfavorite(10L);

        verify(folderFavoriteMapper).updateById(argThat(entity ->
                Long.valueOf(99L).equals(entity.getId())
                        && Boolean.TRUE.equals(entity.getDeleted())
        ));
        verify(auditService).record(argThat(command ->
                "FOLDER".equals(command.getModuleCode())
                        && "UNFAVORITE".equals(command.getOperationType())
                        && Long.valueOf(10L).equals(command.getBizId())
                        && "项目资料".equals(command.getObjectName())
                        && "employee 取消收藏文件夹《项目资料》".equals(command.getActionSummary())
        ));
    }

    @Test
    void favoriteFolderIsIdempotentWhenFavoriteAlreadyExists() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        FolderEntity folder = folder(10L, "项目资料");
        FolderFavoriteEntity existing = new FolderFavoriteEntity();
        existing.setId(99L);
        existing.setFolderId(10L);
        existing.setUserId(7L);
        existing.setDeleted(false);

        when(folderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(folder);
        doNothing().when(folderPermissionService).checkView(folder);
        when(folderFavoriteMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        service.favorite(10L);

        verify(folderFavoriteMapper, never()).insert(any(FolderFavoriteEntity.class));
        verify(auditService, never()).record(any());
    }

    @Test
    void listFavoritesReturnsViewableFoldersOnly() {
        UserContext.set(new UserContext.UserInfo(7L, "employee", List.of("EMPLOYEE"), 100L));

        FolderFavoriteEntity favorite = new FolderFavoriteEntity();
        favorite.setId(99L);
        favorite.setFolderId(10L);
        favorite.setUserId(7L);

        FolderEntity folder = folder(10L, "项目资料");

        when(folderFavoriteMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(favorite));
        when(folderMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(folder));
        when(folderPermissionService.canView(folder)).thenReturn(true);

        var result = service.listFavorites();

        assertEquals(1, result.size());
        assertEquals(99L, result.get(0).getFavoriteId());
        assertEquals("项目资料", result.get(0).getFolderName());
    }

    private static FolderEntity folder(Long id, String name) {
        FolderEntity folder = new FolderEntity();
        folder.setId(id);
        folder.setName(name);
        folder.setLevel(1);
        folder.setDeleted(false);
        return folder;
    }
}
