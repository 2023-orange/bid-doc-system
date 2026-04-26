package com.example.biddoc.common.result;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {

    private List<T> list;
    private long page;
    private long size;
    private long total;
    private long totalPages;
    private boolean hasNext;

    public PageResponse(List<T> list, long page, long size, long total, long totalPages, boolean hasNext) {
        this.list = list;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
    }

    /**
     * Compatible with the previous list + pagination constructor.
     */
    @Deprecated
    public PageResponse(List<T> list, Pagination pagination) {
        this(
                list,
                pagination != null ? pagination.getPage() : 1,
                pagination != null ? pagination.getSize() : 10,
                pagination != null ? pagination.getTotal() : 0,
                pagination != null ? pagination.getTotalPages() : 0,
                pagination != null && pagination.isHasNext()
        );
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getRecords(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages(),
                page.getCurrent() < page.getPages()
        );
    }

    /**
     * Compatible with the previous nested pagination view.
     */
    @Deprecated
    public Pagination getPagination() {
        return new Pagination(page, size, total, totalPages, hasNext);
    }

    @Data
    @AllArgsConstructor
    public static class Pagination {
        private long page;
        private long size;
        private long total;
        private long totalPages;
        private boolean hasNext;
    }
}
