package com.wcs.monitor.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConnectStatus {

    DISCONNECTED("DISCONNECTED", "未连接"),

    CONNECTING("CONNECTING", "连接中"),

    CONNECTED("CONNECTED", "已连接"),

    CONNECT_FAILED("CONNECT_FAILED", "连接失败"),

    CONNECT_TIMEOUT("CONNECT_TIMEOUT", "连接超时");

    @EnumValue
    @JsonValue
    private final String code;

    private final String description;

    @JsonCreator
    public static ConnectStatus fromCode(String value) {
        for (ConnectStatus status : values()) {
            if (status.code.equals(value) || status.description.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知连接状态: " + value);
    }
}
