package com.wcs.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wcs.monitor.enums.MonitorTaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 堆垛机状态监控任务
 * execCount 为 null 或 0 表示持续执行；否则执行到指定次数后自动完成。
 */
@Data
@TableName("monitor_task")
public class MonitorTask {

    public static final String TYPE_STACKER_MONITOR = "STACKER_MONITOR";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;

    private String taskType;

    private String taskName;

    private Long deviceId;

    private Integer execCount;

    private Integer intervalSeconds;

    private MonitorTaskStatus status;

    private Integer executedCount;

    private LocalDateTime lastRunTime;

    private String remark;

    private LocalDateTime createTime;
}
