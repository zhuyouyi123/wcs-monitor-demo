package com.wcs.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_comm_binding")
public class DeviceCommBinding {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deviceId;

    private Long configId;

    private LocalDateTime createTime;
}
