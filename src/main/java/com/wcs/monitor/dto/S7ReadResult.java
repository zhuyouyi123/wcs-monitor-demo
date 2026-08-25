package com.wcs.monitor.dto;

import lombok.Data;

import java.util.List;

@Data
public class S7ReadResult {

    private Integer dbNumber;

    private Integer start;

    private Integer size;

    private String dataType;

    private String hex;

    private List<String> values;
}
