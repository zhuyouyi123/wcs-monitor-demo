package com.wcs.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alarm_log")
public class AlarmLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceCode;

    private String alarmType;

    private String alarmMsg;

    private String alarmLevel;

    private LocalDateTime alarmTime;

    private Integer handleStatus;

    private String handleRemark;
}
