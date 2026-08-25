package com.wcs.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("storage_location")
public class StorageLocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String locationCode;

    private String zone;

    private Integer rowNo;

    private Integer colNo;

    private Integer levelNo;

    private String locationType;

    private Boolean isEmpty;

    private String palletNo;

    private Boolean enabled;
}
