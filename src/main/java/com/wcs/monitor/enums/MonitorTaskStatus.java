package com.wcs.monitor.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MonitorTaskStatus {

    RUNNING("RUNNING", "运行中"),

    STOPPED("STOPPED", "已停止"),

    FINISHED("FINISHED", "已完成");

    @EnumValue
    @JsonValue
    private final String code;

    private final String label;

    @JsonCreator
    public static MonitorTaskStatus fromCode(String value) {
        for (MonitorTaskStatus status : values()) {
            if (status.code.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知任务状态: " + value);
    }
}
