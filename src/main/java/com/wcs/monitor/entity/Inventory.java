package com.wcs.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String palletNo;

    private String materialCode;

    private String materialName;

    private BigDecimal quantity;

    private String locationCode;

    private LocalDateTime inboundTime;

    private LocalDateTime updateTime;
}
