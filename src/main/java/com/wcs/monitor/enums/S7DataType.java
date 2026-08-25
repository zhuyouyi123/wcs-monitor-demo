package com.wcs.monitor.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum S7DataType {

    BYTE("BYTE", "字节 BYTE", 1),

    INT("INT", "整数 INT", 2),

    DINT("DINT", "双整数 DINT", 4),

    REAL("REAL", "实数 REAL", 4);

    @EnumValue
    @JsonValue
    private final String code;

    private final String label;

    private final int length;

    @JsonCreator
    public static S7DataType fromCode(String value) {
        for (S7DataType type : values()) {
            if (type.code.equals(value) || type.label.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知数据类型: " + value);
    }
}
