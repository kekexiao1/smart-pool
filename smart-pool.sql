create database smart_pool;

CREATE TABLE IF NOT EXISTS `pool_config_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_name` varchar(64) NOT NULL COMMENT '应用名称（服务名），用于区分不同微服务',
    `pool_name` varchar(64) NOT NULL COMMENT '线程池名称',
    `old_config` text COMMENT '修改前的配置快照',
    `new_config` text COMMENT '修改后的配置快照',
    `change_type` varchar(16) DEFAULT 'UPDATE' COMMENT '变更类型：UPDATE(更新), INIT(初始化)',
    `operator` varchar(64) DEFAULT 'SYSTEM' COMMENT '操作人（Nacos一般通过ConfigChange获取，若无则为系统自动）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更发生时间',
    `source` varchar(16) NOT NULL DEFAULT 'NACOS' COMMENT '配置变更源',
    PRIMARY KEY (`id`),
    KEY `idx_app_pool` (`app_name`, `pool_name`) COMMENT '用于查询某个应用下某个线程池的变更历史',
    KEY `idx_create_time` (`create_time`) COMMENT '用于按时间清理或查询日志'
) ENGINE = InnoDB AUTO_INCREMENT = 244 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '线程池配置变更日志表'

CREATE TABLE IF NOT EXISTS `pool_alert_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_name` varchar(64) NOT NULL COMMENT '应用名称',
    `pool_name` varchar(64) NOT NULL COMMENT '线程池名称',
    `alert_type` varchar(32) NOT NULL COMMENT '告警类型：QUEUE_CAPACITY(队列容量), REJECT_POLICY(拒绝策略), DEADLOCK(死锁)等',
    `alert_level` varchar(16) DEFAULT 'WARNING' COMMENT '告警级别：INFO, WARNING, CRITICAL',
    `content` varchar(1024) DEFAULT NULL COMMENT '告警详情信息',
    `status` tinyint NOT NULL DEFAULT '0' COMMENT '处理状态：0-未读/未处理, 1-已读/已处理',
    `handler` varchar(64) DEFAULT NULL COMMENT '处理人',
    `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '告警发生时间',
    PRIMARY KEY (`id`),
    KEY `idx_app_pool` (`app_name`, `pool_name`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB AUTO_INCREMENT = 67 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '线程池告警日志表'