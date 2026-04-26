package com.example.biddoc.common.result;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQuery {

    @Min(value = 1, message = "page不能小于1")
    private long page = 1;

    @Min(value = 1, message = "size不能小于1")
    @Max(value = 100, message = "size不能大于100")
    private long size = 10;
}
