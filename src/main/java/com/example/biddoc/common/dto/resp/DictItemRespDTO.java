package com.example.biddoc.common.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictItemRespDTO {

    private String type;
    private String code;
    private String name;
    private String description;
    private Integer sort;
}
