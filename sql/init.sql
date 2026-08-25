/* =============================================
   WCS 管理平台数据库初始化脚本
   适用：SQL Server 2022 Express（本地默认实例）
   ============================================= */

IF DB_ID('wcs_manager') IS NULL
    CREATE DATABASE wcs_manager COLLATE Chinese_PRC_CI_AS;
GO

USE wcs_manager;
GO

/* ---------- 用户表 ---------- */
CREATE TABLE sys_user (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    username     NVARCHAR(50)  NOT NULL UNIQUE,
    password     NVARCHAR(100) NOT NULL,
    real_name    NVARCHAR(50)  NULL,
    role         NVARCHAR(20)  NOT NULL DEFAULT 'user',
    status       TINYINT       NOT NULL DEFAULT 1,
    remark       NVARCHAR(200) NULL,
    create_time  DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME(),
    update_time  DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO

/* ---------- 设备表 ---------- */
CREATE TABLE device_info (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    device_code     VARCHAR(50)   NOT NULL UNIQUE,
    device_name     NVARCHAR(100) NOT NULL,
    device_type     NVARCHAR(20)  NULL,
    ip_address      VARCHAR(50)   NULL,
    port            INT           NULL,
    rack_levels     INT           NULL,
    rack_cols       INT           NULL,
    status          NVARCHAR(20)  NOT NULL DEFAULT 'DISCONNECTED',
    last_heartbeat  DATETIME2(0)  NULL,
    remark          NVARCHAR(200) NULL,
    create_time     DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO

/* ---------- 任务表 ---------- */
CREATE TABLE wcs_task (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    task_no       VARCHAR(32)   NOT NULL UNIQUE,
    task_type     NVARCHAR(10)  NOT NULL,
    pallet_no     VARCHAR(32)   NULL,
    from_location VARCHAR(32)   NULL,
    to_location   VARCHAR(32)   NULL,
    priority      INT           NOT NULL DEFAULT 5,
    status        NVARCHAR(20)  NOT NULL DEFAULT N'等待中',
    device_code   VARCHAR(50)   NULL,
    start_time    DATETIME2(0)  NULL,
    end_time      DATETIME2(0)  NULL,
    create_time   DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX idx_task_status ON wcs_task (status);
CREATE INDEX idx_task_create ON wcs_task (create_time);
GO

/* ---------- 设备 IP 唯一约束 ---------- */
CREATE UNIQUE INDEX uq_device_ip ON device_info (ip_address) WHERE ip_address IS NOT NULL;
GO

/* ---------- 库位表 ---------- */
CREATE TABLE storage_location (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    location_code VARCHAR(32)  NOT NULL UNIQUE,
    zone          NVARCHAR(20) NULL,
    row_no        INT          NOT NULL,
    col_no        INT          NOT NULL,
    level_no      INT          NOT NULL,
    location_type NVARCHAR(20) NOT NULL DEFAULT N'存储位',
    is_empty      BIT          NOT NULL DEFAULT 1,
    pallet_no     VARCHAR(32)  NULL,
    enabled       BIT          NOT NULL DEFAULT 1
);
GO

/* ---------- 库存表 ---------- */
CREATE TABLE inventory (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    pallet_no     VARCHAR(32)   NOT NULL,
    material_code VARCHAR(50)   NULL,
    material_name NVARCHAR(100) NULL,
    quantity      DECIMAL(18,2) NOT NULL DEFAULT 0,
    location_code VARCHAR(32)   NULL,
    inbound_time  DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME(),
    update_time   DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX idx_inv_pallet ON inventory (pallet_no);
CREATE INDEX idx_inv_location ON inventory (location_code);
GO

/* ---------- 告警日志表 ---------- */
CREATE TABLE alarm_log (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    device_code    VARCHAR(50)   NULL,
    alarm_type     NVARCHAR(30)  NULL,
    alarm_msg      NVARCHAR(500) NULL,
    alarm_level    NVARCHAR(10)  NOT NULL DEFAULT N'一般',
    alarm_time     DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME(),
    handle_status  TINYINT       NOT NULL DEFAULT 0,
    handle_remark  NVARCHAR(200) NULL
);
GO
CREATE INDEX idx_alarm_time ON alarm_log (alarm_time);
GO

/* ---------- 操作日志表 ---------- */
CREATE TABLE operation_log (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    username   NVARCHAR(50)  NULL,
    module     NVARCHAR(50)  NULL,
    operation  NVARCHAR(200) NULL,
    detail     NVARCHAR(MAX) NULL,
    op_time    DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO

/* ---------- 通信测试配置表 ---------- */
CREATE TABLE comm_test_config (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    config_name  NVARCHAR(50)  NOT NULL,
    device_type  NVARCHAR(20)  NOT NULL,
    db_number    INT           NOT NULL DEFAULT 1,
    start_offset INT           NOT NULL DEFAULT 0,
    read_length  INT           NOT NULL DEFAULT 4,
    data_type    VARCHAR(20)   NOT NULL DEFAULT 'BYTE',
    dict_key     VARCHAR(50)   NULL,
    remark       NVARCHAR(200) NULL,
    create_time  DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uq_comm_config_name ON comm_test_config (device_type, config_name);
GO
CREATE UNIQUE INDEX uq_comm_config_params ON comm_test_config (device_type, db_number, start_offset, read_length, data_type);
GO

/* ---------- 设备-通信配置绑定表 ---------- */
CREATE TABLE device_comm_binding (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    device_id   INT           NOT NULL,
    config_id   INT           NOT NULL,
    create_time DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uq_device_config ON device_comm_binding (device_id, config_id);
GO
CREATE INDEX idx_binding_device ON device_comm_binding (device_id);
GO
CREATE INDEX idx_binding_config ON device_comm_binding (config_id);
GO

/* ---------- 输送线节点表 ---------- */
CREATE TABLE conveyor_node (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    node_code   VARCHAR(50)   NOT NULL UNIQUE,
    node_name   NVARCHAR(100) NOT NULL,
    device_id   INT           NOT NULL,
    node_type   NVARCHAR(20)  NULL,
    address     VARCHAR(100)  NULL,
    remark      NVARCHAR(200) NULL,
    create_time DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX idx_node_device ON conveyor_node (device_id);
GO

/* ---------- 系统配置表 ---------- */
CREATE TABLE sys_config (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    config_key   VARCHAR(50)   NOT NULL UNIQUE,
    config_value NVARCHAR(200) NULL,
    update_time  DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO

INSERT INTO sys_config (config_key, config_value) VALUES
('systemName',       N'WCS 仓库控制系统'),
('warehouseCode',    N'WH001'),
('pageSize',         N'20'),
('connectTimeout',   N'5000'),
('connIdleTimeout',  N'60'),
('autoDispatch',     N'true'),
('dispatchInterval', N'5'),
('maxTaskPerDevice', N'2'),
('refreshInterval',  N'5'),
('alarmSound',       N'false'),
('opLogKeepDays',    N'90'),
('alarmLogKeepDays', N'30');
GO

/* ---------- 堆垛机状态监控任务表 ---------- */
CREATE TABLE monitor_task (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    task_no          VARCHAR(40)   NOT NULL UNIQUE,
    task_type        NVARCHAR(30)  NOT NULL DEFAULT N'STACKER_MONITOR',
    task_name        NVARCHAR(100) NOT NULL,
    device_id        INT           NOT NULL,
    exec_count       INT           NULL,
    interval_seconds INT           NOT NULL DEFAULT 5,
    status           NVARCHAR(20)  NOT NULL DEFAULT 'STOPPED',
    executed_count   INT           NOT NULL DEFAULT 0,
    last_run_time    DATETIME2(0)  NULL,
    remark           NVARCHAR(200) NULL,
    create_time      DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX idx_mtask_device ON monitor_task (device_id);
CREATE INDEX idx_mtask_status ON monitor_task (status);
GO

/* ---------- 监控任务采集数据表 ---------- */
CREATE TABLE monitor_task_data (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    task_id      INT           NOT NULL,
    device_id    INT           NOT NULL,
    config_id    INT           NULL,
    config_name  NVARCHAR(50)  NULL,
    db_number    INT           NULL,
    start_offset INT           NULL,
    data_type    VARCHAR(20)   NULL,
    raw_value    NVARCHAR(200) NULL,
    dict_label   NVARCHAR(100) NULL,
    dict_color   VARCHAR(20)   NULL,
    collect_time DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE INDEX idx_mtdata_task_time ON monitor_task_data (task_id, collect_time DESC);
GO

/* ---------- 数据字典表 ---------- */
CREATE TABLE sys_dict_item (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    dict_name   NVARCHAR(100) NOT NULL,
    dict_key    VARCHAR(50)   NOT NULL,
    dict_value  VARCHAR(50)   NOT NULL,
    dict_label  NVARCHAR(100) NOT NULL,
    dict_color  VARCHAR(20)   NULL,
    sort_order  INT           NOT NULL DEFAULT 0,
    create_time DATETIME2(0)  NOT NULL DEFAULT SYSDATETIME()
);
GO
CREATE UNIQUE INDEX uq_dict_item ON sys_dict_item (dict_key, dict_value);
GO

INSERT INTO sys_dict_item ([dict_name], [dict_key], [dict_value], [dict_label], [dict_color], [sort_order]) VALUES
(N'堆垛机采集状态', N'STACKER_COLLECT_STATUS', N'0', N'其它', '#909399', 0),
(N'堆垛机采集状态', N'STACKER_COLLECT_STATUS', N'1', N'空闲', '#409EFF', 1),
(N'堆垛机采集状态', N'STACKER_COLLECT_STATUS', N'2', N'运行', '#67C23A', 2),
(N'堆垛机采集状态', N'STACKER_COLLECT_STATUS', N'3', N'故障', '#F56C6C', 3);
GO

INSERT INTO sys_user (username, password, real_name, role)
VALUES (N'admin', N'123456', N'管理员', N'admin');
GO
