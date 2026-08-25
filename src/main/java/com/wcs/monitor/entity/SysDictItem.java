package com.wcs.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据字典项：按 dictKey 分组，value -> label 含义映射
 */
@Data
@TableName("sys_dict_item")
public class SysDictItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String dictName;

    private String dictKey;

    private String dictValue;

    private String dictLabel;

    /** 显示颜色，#RRGGBB，可空 */
    private String dictColor;

    private Integer sortOrder;

    private LocalDateTime createTime;
}
