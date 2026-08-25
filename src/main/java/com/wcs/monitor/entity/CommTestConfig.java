package com.wcs.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wcs.monitor.enums.DeviceType;
import com.wcs.monitor.enums.S7DataType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comm_test_config")
public class CommTestConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configName;

    private DeviceType deviceType;

    private Integer dbNumber;

    private Integer startOffset;

    private Integer readLength;

    private S7DataType dataType;

    /** 关联的数据字典键（可选）：读取值按 字典值->含义 自动转换 */
    private String dictKey;

    private String remark;

    private LocalDateTime createTime;
}
