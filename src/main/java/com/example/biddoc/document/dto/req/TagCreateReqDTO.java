package com.example.biddoc.document.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TagCreateReqDTO {

    @NotBlank(message = "name不能为空")
    private String name;
}
