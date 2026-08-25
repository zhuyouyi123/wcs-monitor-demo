package com.wcs.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wcs_task")
public class WcsTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;

    private String taskType;

    private String palletNo;

    private String fromLocation;

    private String toLocation;

    private Integer priority;

    private String status;

    private String deviceCode;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;
}
