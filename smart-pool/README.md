# Smart Pool - 动态线程池管理平台

## 项目简介

Smart Pool 是一个轻量级的动态线程池管理平台，支持线程池参数动态调整、实时监控、告警通知等功能。基于 Spring Boot 3.x 构建，提供可视化监控界面，帮助开发者更好地管理和监控应用中的线程池。

## 功能特性

- **动态参数调整**: 支持运行时动态修改线程池核心参数（核心线程数、最大线程数、队列容量等）
- **实时监控**: 提供线程池运行指标的实时监控和历史数据查询
- **告警通知**: 支持多种告警渠道（钉钉、企业微信、邮件）
- **拒绝策略**: 内置多种拒绝策略（本地磁盘存储、Redis、MQ）
- **配置中心集成**: 支持 Nacos 配置中心，实现配置统一管理
- **可视化界面**: 提供 React 前端监控面板

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2.12
- Spring Cloud Alibaba 2023.0.1.3
- Nacos (配置中心/服务发现)
- Redis (指标存储)
- MySQL (日志持久化)
- RocketMQ (消息队列)
- MyBatis (ORM)

### 前端
- React 18
- Vite
- Ant Design
- @ant-design/charts

## 项目结构

```
smart-pool/
├── smart-pool-core/           # 核心模块 - 动态线程池实现
├── smart-pool-metrics/        # 指标模块 - 线程池指标采集
├── smart-pool-alert/          # 告警模块 - 告警规则与通知
├── smart-pool-admin-server/   # 管理服务 - 后台管理API
├── smart-pool-starter/        # 启动器 - Spring Boot Starter
└── pom.xml
```

### 模块说明

| 模块 | 说明 |
|------|------|
| smart-pool-core | 核心模块，提供动态线程池 `DynamicThreadPoolExecutor` 的实现，支持参数动态调整 |
| smart-pool-metrics | 指标采集模块，负责采集线程池运行指标（活跃线程数、队列大小、完成任务数等） |
| smart-pool-alert | 告警模块，提供告警规则引擎和多渠道通知能力 |
| smart-pool-admin-server | 管理后台服务，提供 REST API 和前端页面 |
| smart-pool-starter | Spring Boot Starter，简化集成 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.x (可选)

### 编译项目

```bash
cd smart-pool
mvn clean install -DskipTests
```

### 配置数据库

创建数据库并执行初始化脚本：

```sql
CREATE DATABASE smart_pool;
```

### 启动服务

1. 修改 `smart-pool-admin-server/src/main/resources/application.yml` 中的数据库、Redis 配置

2. 启动管理服务：

```bash
cd smart-pool-admin-server
mvn spring-boot:run
```

服务启动后访问：`http://localhost:8081/admin`

### 前端启动

```bash
cd front
npm install
npm run dev
```

## 使用方式

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.xiao</groupId>
    <artifactId>smart-pool-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

### 2. 配置线程池

```yaml
executors:
  order-pool:
    corePoolSize: 5
    maximumPoolSize: 5
    keepAliveTime: 60
    unit: SECONDS
    queueCapacity: 180
    rejectedHandlerClass: CallerRunsPolicy
  pay-pool:
    corePoolSize: 10
    maximumPoolSize: 20
    keepAliveTime: 60
    unit: SECONDS
    queueCapacity: 512
    rejectedHandlerClass: CallerRunsPolicy
```

### 3. 使用线程池

```java
@Autowired
private ThreadPoolRegistry threadPoolRegistry;

public void execute() {
    DynamicThreadPoolExecutor executor = threadPoolRegistry.getExecutor("order-pool");
    executor.execute(() -> {
        // 业务逻辑
    });
}
```

## 监控指标

| 指标 | 说明 |
|------|------|
| activeCount | 活跃线程数 |
| poolSize | 当前线程池大小 |
| corePoolSize | 核心线程数 |
| maximumPoolSize | 最大线程数 |
| queueSize | 队列当前大小 |
| queueCapacity | 队列容量 |
| completedTaskCount | 已完成任务数 |
| rejectCount | 拒绝任务数 |

## 告警配置

```yaml
pool-alert:
  alert:
    notifiers:
      - type: EMAIL
        enabled: true
        alertTypes: ["CAPACITY", "LIVENESS", "RUN_TIMEOUT", "REJECT" ]
        email:
          from: "xxxxxxx@qq.com"
          to: "xxxxxxx@qq.com"
          host: "smtp.qq.com"
          port: 587
          username: "xxxxxxx@qq.com"
          password: "xxxxxxx" # 邮箱认证码
          ssl: true
      
      - type: DING
        enabled: false
        alertTypes: []
        webhook:
          url: "https://oapi.dingtalk.com/robot/send?access_token=xxx"
          secret: "SECxxx"

      - type: WECHAT
        enabled: false
        alertTypes: []
        webhook:
          url: "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx"

thread-pool:
  default-config:
    rules:
      - type: CAPACITY
        threshold: 80
        count: 3
        period: 60
        silencePeriod: 180
        enabled: false
      - type: LIVENESS
        threshold: 80
        count: 2
        period: 60
        silencePeriod: 180
        enabled: false
      - type: RUN_TIMEOUT
        threshold: 10
        count: 1
        silencePeriod: 180
        enabled: false
      - type: REJECT
        period: 60
        silencePeriod: 300
        enabled: false
    monitor-interval: 5000

  pools:
    order-pool:
      rules:
        - type: CAPACITY
          threshold: 75
          count: 3
          period: 60
          silencePeriod: 180
          enabled: true
        - type: LIVENESS
          threshold: 70
          count: 2
          period: 60
          silencePeriod: 180
          enabled: true
        - type: RUN_TIMEOUT
          threshold: 5
          count: 1
          silencePeriod: 180
          enabled: true
        - type: REJECT
          period: 30
          silencePeriod: 300
          enabled: true
      monitor-interval: 3000
```

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/thread-pool/list | GET | 获取线程池列表 |
| /api/thread-pool/config | PUT | 更新线程池配置 |
| /api/thread-pool/metrics | GET | 获取实时指标 |
| /api/thread-pool/history | GET | 获取历史指标 |
| /api/log/query | GET | 查询日志 |

## 许可证

MIT License
