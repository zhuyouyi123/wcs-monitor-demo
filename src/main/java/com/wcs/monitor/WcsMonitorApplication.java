package com.wcs.monitor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wcs.monitor.mapper")
public class WcsMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(WcsMonitorApplication.class, args);
    }

}
