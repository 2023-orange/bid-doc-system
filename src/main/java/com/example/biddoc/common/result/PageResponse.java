package com.example.biddoc.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> list;
    private Pagination pagination;

    @Data
    @AllArgsConstructor
    public static class Pagination {
        private long page;
        private long size;
        private long total;
        private boolean hasNext;
    }
}
