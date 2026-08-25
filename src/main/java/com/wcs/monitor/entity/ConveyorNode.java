package com.wcs.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conveyor_node")
public class ConveyorNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String nodeCode;

    private String nodeName;

    private Long deviceId;

    private String nodeType;

    private String address;

    private String remark;

    private LocalDateTime createTime;
}
