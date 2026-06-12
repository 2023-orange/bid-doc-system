package com.example.biddoc.project.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("bid_project_no_sequence")
public class ProjectNoSequenceEntity {

    @TableId
    private Long id;
    private Long deptId;
    private LocalDate bizDate;
    private Integer currentSeq;
}
