package com.example.biddoc.auth.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailRespDTO {

    private Long id;
    private String username;
    private String realName;
    private String email;
    private String mobile;
    private Long deptId;
    private Integer jobLevel;
    private Integer status;

    /** 当前生效的角色码列表 */
    private List<String> roleCodes;
}

