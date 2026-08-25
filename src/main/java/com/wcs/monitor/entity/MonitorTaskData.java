package com.wcs.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 监控任务采集数据：每次执行按绑定配置逐条落库
 */
@Data
@TableName("monitor_task_data")
public class MonitorTaskData {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long deviceId;

    private Long configId;

    private String configName;

    private Integer dbNumber;

    private Integer startOffset;

    private String dataType;

    private String rawValue;

    private LocalDateTime collectTime;
}
