package com.wcs.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wcs.monitor.enums.ConnectStatus;
import com.wcs.monitor.enums.DeviceType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_info")
public class DeviceInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceCode;

    private String deviceName;

    private DeviceType deviceType;

    private String ipAddress;

    private Integer port;

    /** 货架层数（可视化二维展示用） */
    private Integer rackLevels;

    /** 货架列数（可视化二维展示用） */
    private Integer rackCols;

    private ConnectStatus status;

    private LocalDateTime lastHeartbeat;

    private String remark;

    private LocalDateTime createTime;
}
