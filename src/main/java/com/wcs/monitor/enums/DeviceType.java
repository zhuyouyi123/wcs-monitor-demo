package com.wcs.monitor.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeviceType {

    STACKER("STACKER", "堆垛机"),

    CONVEYOR("CONVEYOR", "输送线");

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    @JsonCreator
    public static DeviceType fromCode(String value) {
        for (DeviceType type : values()) {
            if (type.code.equals(value) || type.description.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知设备类型: " + value);
    }
}
