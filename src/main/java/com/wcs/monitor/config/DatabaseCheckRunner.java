package com.wcs.monitor.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseCheckRunner implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        log.info("========== 数据库连接检测开始 ==========");
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            log.info("[DB-CHECK] 状态: 连接成功 √");
            log.info("[DB-CHECK] 数据库产品: {} {}", meta.getDatabaseProductName(),
                    meta.getDatabaseProductVersion().split("\n")[0]);
            log.info("[DB-CHECK] 当前数据库: {}", conn.getCatalog());
            log.info("[DB-CHECK] 驱动: {} {}", meta.getDriverName(), meta.getDriverVersion());
            log.info("[DB-CHECK] 连接地址: {}", meta.getURL());
            log.info("========== 数据库连接检测通过 ==========");
        } catch (Exception e) {
            log.error("[DB-CHECK] 状态: 连接失败 ×");
            log.error("[DB-CHECK] 原因: {}", e.getMessage());
            log.error("========== 请检查 application.yml 数据源配置或 SQL Server 服务是否运行 ==========");
        }
    }
}
