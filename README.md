# Smart Pool - 动态线程池管理平台

## 项目简介

Smart Pool 是一个轻量级的动态线程池管理平台，支持线程池参数运行时动态调整、实时监控、多渠道告警通知、拒绝任务持久化等功能。基于 Spring Boot 3.2 构建，集成 Nacos 配置中心实现配置统一管理，提供 React 可视化监控面板，帮助开发者更好地管理和监控应用中的线程池。

## 核心特性

| 特性 | 说明 |
|------|------|
| 动态参数调整 | 运行时动态修改线程池核心参数（核心线程数、最大线程数、队列容量、拒绝策略等），无需重启应用 |
| 配置中心集成 | 基于 Nacos 配置中心，配置变更实时推送，支持 Nacos 控制台和管理后台双通道修改 |
| 实时监控 | 采集线程池运行指标，支持 Prometheus + Grafana 监控和自建前端监控面板 |
| 多渠道告警 | 支持邮件、钉钉、企业微信三种告警渠道，可按告警类型灵活配置通知渠道 |
| 告警规则引擎 | 内置队列容量、活跃度、执行超时、拒绝任务四种告警规则，支持阈值、触发次数、静默期等细粒度配置 |
| 拒绝任务持久化 | 内置 Redis、RocketMQ、本地磁盘三种拒绝任务持久化策略，支持降级兜底，防止任务丢失 |
| 任务执行监控 | 自动记录任务等待时间和执行时间，支持超时检测和告警 |
| 可视化界面 | React 前端监控面板，支持线程池配置管理、实时监控、历史趋势、日志查询 |
| 配置变更审计 | 记录所有配置变更日志，支持按线程池、操作类型、时间范围查询 |

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.2.12 | 应用框架 |
| Spring Cloud Alibaba | 2023.0.1.3 | Nacos 配置中心 / 服务发现 |
| Nacos | 2.x | 配置中心 / 服务发现 |
| Redis | 6.0+ | 指标存储 / 拒绝任务持久化 |
| MySQL | 8.0+ | 日志持久化 |
| RocketMQ | 5.x | 拒绝任务消息队列 |
| MyBatis | 3.0.3 | ORM 框架 |
| Micrometer | 1.12.0 | 指标采集，对接 Prometheus |
| Lombok | 1.18.38 | 简化代码 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18 | UI 框架 |
| Vite | 4.5 | 构建工具 |
| Ant Design | 5.12 | UI 组件库 |
| @ant-design/charts | 2.6 | 图表组件 |
| React Router | 6.20 | 路由管理 |
| Axios | 1.6 | HTTP 客户端 |
| Tailwind CSS | 3.3 | 样式框架 |

## 项目结构

```
Project/
├── smart-pool/                          # 后端项目根目录
│   ├── smart-pool-core/                 # 核心模块
│   │   └── src/main/java/.../smartpoolcore/
│   │       ├── core/
│   │       │   ├── executor/            # DynamicThreadPoolExecutor 动态线程池
│   │       │   ├── handler/             # ConfigChangeHandler 配置变更处理器
│   │       │   ├── manager/             # DynamicThreadPoolManager 管理器
│   │       │   │                        # TaskExecutionCallbackManager 回调管理器
│   │       │   ├── registry/            # ThreadPoolRegistry 注册中心
│   │       │   └── task/                # MonitorablePoolTask 可监控任务
│   │       ├── config/                  # DynamicCapacityBlockingQueue 动态队列
│   │       │                            # CountingRejectedExecutionHandler 计数拒绝策略
│   │       │                            # NamedThreadFactory 命名线程工厂
│   │       ├── listener/                # ConfigSourceListener 配置源监听接口
│   │       │   └── nacos/               # NacosConfigListener Nacos配置监听
│   │       ├── reject/                  # 拒绝策略
│   │       │   ├── local/               # LocalDiskRejectPolicy 本地磁盘
│   │       │   ├── redis/               # RedisRejectPolicy Redis持久化
│   │       │   └── mq/                  # MQRejectPolicy RocketMQ持久化
│   │       └── model/                   # DTO / Entity / Event
│   │
│   ├── smart-pool-metrics/              # 指标模块
│   │   └── src/main/java/.../smartpoolmetrics/
│   │       ├── metrics/                 # ThreadPoolMetricCollector 指标采集器
│   │       │                            # ThreadPoolMetrics 指标模型
│   │       ├── monitor/                 # ThreadPoolMonitor Micrometer指标注册
│   │       ├── callback/                # MetricsTaskExecutionCallback 执行回调
│   │       └── common/constant/         # ThreadPoolIndicatorEnum 指标枚举
│   │
│   ├── smart-pool-alert/                # 告警模块
│   │   └── src/main/java/.../smartpoolalert/
│   │       ├── monitor/engine/          # AlertEngine 告警引擎
│   │       ├── manager/                 # AlertManager 告警管理器
│   │       ├── notification/            # AlertNotifier 通知器接口
│   │       │   ├── EmailAlertNotifier   # 邮件通知
│   │       │   ├── DingTalkAlertNotifier# 钉钉通知
│   │       │   └── WeChatAlertNotifier  # 企业微信通知
│   │       ├── rule/                    # AlertRule 告警规则
│   │       ├── context/                 # AlertContext 告警上下文
│   │       └── config/                  # AlertProperties 告警配置
│   │
│   ├── smart-pool-admin-server/         # 管理后台服务
│   │   └── src/main/java/.../smartpooladminserver/
│   │       ├── controller/              # REST API 控制器
│   │       ├── service/                 # 业务逻辑层
│   │       ├── mapper/                  # MyBatis Mapper
│   │       ├── model/                   # DTO / VO
│   │       ├── config/                  # Redis / Jackson 配置
│   │       └── consumer/                # RocketMQ 消费者
│   │
│   ├── smart-pool-starter/              # Spring Boot Starter
│   │   └── SmartPoolAutoConfiguration   # 自动配置类
│   │
│   ├── smart-pool.sql                   # 数据库初始化脚本
│   ├── grafana-dashboard.json           # Grafana 监控面板配置
│   └── .env.example                     # 环境变量示例
│
└── front/                               # 前端项目
    └── src/
        ├── pages/
        │   ├── ThreadPoolConfigList.jsx  # 配置管理列表
        │   ├── ThreadPoolConfigForm.jsx  # 配置新增/编辑表单
        │   ├── ThreadPoolMonitor.jsx     # 实时监控总览
        │   ├── ThreadPoolMonitorDetail.jsx# 监控详情（趋势图）
        │   └── LogQuery.jsx             # 日志查询（配置日志/告警日志）
        ├── components/
        │   └── Header.jsx               # 导航头部
        └── services/
            └── api.js                    # API 请求封装
```

### 模块依赖关系

```
smart-pool-starter
    ├── smart-pool-core        (核心线程池实现)
    ├── smart-pool-metrics     (依赖 core，指标采集)
    └── smart-pool-alert       (依赖 metrics，告警通知)

smart-pool-admin-server
    └── smart-pool-starter     (依赖 starter，管理后台)
```

### 模块说明

| 模块 | 职责 | 核心类 |
|------|------|--------|
| smart-pool-core | 动态线程池核心实现，包括线程池创建、参数动态调整、配置变更处理、拒绝策略、Nacos 配置监听 | `DynamicThreadPoolExecutor`、`ConfigChangeHandler`、`ThreadPoolRegistry`、`NacosConfigListener` |
| smart-pool-metrics | 线程池指标采集与 Micrometer 集成，注册 Prometheus 指标，记录任务执行/等待时间 | `ThreadPoolMetricCollector`、`ThreadPoolMonitor` |
| smart-pool-alert | 告警规则引擎与多渠道通知，支持队列容量、活跃度、执行超时、拒绝任务四种告警类型 | `AlertEngine`、`AlertManager`、`EmailAlertNotifier` |
| smart-pool-admin-server | 管理后台服务，提供 REST API、指标 Redis 存储、日志 MySQL 持久化、RocketMQ 消费 | `ThreadPoolConfigController`、`ThreadPoolMetricsController`、`LogQueryController` |
| smart-pool-starter | Spring Boot Starter 自动配置，一键引入 core + metrics + alert | `SmartPoolAutoConfiguration` |

## 快速开始

### 环境要求

| 依赖 | 版本要求 | 必选 |
|------|---------|------|
| JDK | 17+ | 是 |
| Maven | 3.6+ | 是 |
| MySQL | 8.0+ | 是 |
| Redis | 6.0+ | 是 |
| Nacos | 2.x | 否（不使用 Nacos 时需自行实现配置加载） |
| RocketMQ | 5.x | 否（不使用 MQ 拒绝策略时不需要） |

### 编译项目

```bash
cd smart-pool
mvn clean install -DskipTests
```

### 配置数据库

创建数据库并执行初始化脚本：

```bash
mysql -u root -p < smart-pool.sql
```

脚本会创建 `smart_pool` 数据库及以下表：

| 表名 | 说明 |
|------|------|
| pool_config_log | 线程池配置变更日志，记录每次配置变更的前后快照 |
| pool_alert_log | 线程池告警日志，记录告警类型、内容、处理状态 |

### 启动管理服务

1. 修改 `smart-pool-admin-server/src/main/resources/application.yml` 中的数据库、Redis、Nacos 等配置

2. 配置环境变量（可选，用于邮件通知）：

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，填入邮件配置
MAIL_HOST=smtp.qq.com
MAIL_PORT=587
MAIL_USERNAME=your_email@qq.com
MAIL_PASSWORD=your_email_auth_code
```

3. 启动服务：

```bash
cd smart-pool-admin-server
mvn spring-boot:run
```

服务启动后访问：`http://localhost:8081/admin`

### 启动前端

```bash
cd front
npm install
npm run dev
```

前端访问地址：`http://localhost:7071`（自动代理后端 API 到 `localhost:8081`）

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

在 Nacos 配置中心创建配置文件（如 `smart-pool.yaml`），内容如下：

```yaml
executors:
  order-pool:
    corePoolSize: 5
    maximumPoolSize: 10
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
    rejectedHandlerClass: Redis
```

### 3. 使用线程池

```java
@Autowired
private ThreadPoolRegistry threadPoolRegistry;

public void execute() {
    DynamicThreadPoolExecutor executor = threadPoolRegistry.getExecutor("order-pool");
    executor.execute(
        () -> { /* 业务逻辑 */ },
        "task-001",       // taskId: 任务唯一标识
        "ORDER",          // taskType: 任务类型
        "TRADE",          // businessType: 业务类型
        "{\"orderId\":1}" // payload: 任务参数(JSON格式)
    );
}
```

> 通过 `MonitorablePoolTask` 包装，系统会自动记录任务的等待时间和执行时间，无需额外配置。

## 拒绝策略

系统内置 7 种拒绝策略，分为 JDK 标准策略和持久化策略两大类：

### JDK 标准策略

| 策略名 | 配置值 | 说明 |
|--------|--------|------|
| CallerRunsPolicy | `CallerRuns` | 由调用线程执行被拒绝的任务，起到流量控制作用 |
| AbortPolicy | `Abort` | 抛出 RejectedExecutionException，默认策略 |
| DiscardPolicy | `Discard` | 静默丢弃被拒绝的任务 |
| DiscardOldestPolicy | `DiscardOldest` | 丢弃队列中最老的任务，然后重新提交 |

### 持久化策略

| 策略名 | 配置值 | 说明 | 依赖 |
|--------|--------|------|------|
| RedisRejectPolicy | `Redis` | 将拒绝的任务批量异步写入 Redis List，支持本地磁盘降级兜底 | Redis |
| MQRejectPolicy | `Mq` | 将拒绝的任务发送到 RocketMQ，支持最多 3 次重试，失败后降级到本地磁盘 | RocketMQ |
| LocalDiskRejectPolicy | `LocalDisk` | 将拒绝的任务序列化保存到本地磁盘文件 | 无 |

> 持久化策略均继承自 `AbstractRejectPolicy`，采用降级兜底机制：MQ → 本地磁盘 → 日志记录，确保任务不丢失。

## 告警系统

### 告警类型

| 类型 | 枚举值 | 说明 |
|------|--------|------|
| 队列容量 | `CAPACITY` | 队列使用率超过阈值时触发（如 80%） |
| 活跃度 | `LIVENESS` | 活跃线程数占最大线程数比例超过阈值时触发 |
| 执行超时 | `RUN_TIMEOUT` | 任务执行时间超过阈值（秒）时触发 |
| 拒绝任务 | `REJECT` | 出现被拒绝的任务时触发 |
| 配置变更 | `CHANGE` | 线程池配置发生变更时触发 |
| 拒绝策略变更 | `REJECT_POLICY_CHANGE` | 拒绝策略发生变更时触发 |
| 排队超时 | `QUEUE_TIMEOUT` | 任务在队列中等待时间超过阈值时触发 |

### 告警规则参数

| 参数 | 说明 |
|------|------|
| threshold | 告警阈值（百分比或秒数） |
| count | 在统计时间窗口内达到阈值的次数 |
| period | 统计时间窗口（秒） |
| silencePeriod | 告警静默期（秒），静默期内不重复告警 |
| enabled | 是否启用该规则 |

### 通知渠道配置

```yaml
pool-alert:
  alert:
    notifiers:
      - type: EMAIL
        enabled: true
        alertTypes: ["CAPACITY", "LIVENESS", "RUN_TIMEOUT", "REJECT"]
        email:
          from: "xxxxxxx@qq.com"
          to: "xxxxxxx@qq.com"
          host: "smtp.qq.com"
          port: 587
          username: "xxxxxxx@qq.com"
          password: "xxxxxxx"
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
```

> `alertTypes` 为空时表示该渠道接收所有类型的告警；指定类型后仅接收对应类型的告警。

### 告警规则配置

```yaml
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

> 每个线程池可独立配置告警规则，未配置时使用 `default-config` 中的默认规则。

## 监控指标

### Prometheus 指标

| 指标名 | 类型 | 说明 |
|--------|------|------|
| `thread_pool_active_count` | Gauge | 活跃线程数 |
| `thread_pool_current_pool_size` | Gauge | 当前线程池大小 |
| `thread_pool_core_pool_size` | Gauge | 核心线程数 |
| `thread_pool_max_pool_size` | Gauge | 最大线程数 |
| `thread_pool_queue_size` | Gauge | 队列当前大小 |
| `thread_pool_queue_capacity` | Gauge | 队列容量 |
| `thread_pool_queue_remaining_capacity` | Gauge | 队列剩余容量 |
| `thread_pool_task_count` | Counter | 任务总数 |
| `thread_pool_completed_task_count` | Counter | 已完成任务数 |
| `thread_pool_reject_count` | Gauge | 拒绝任务数 |
| `thread_pool_execute_time` | Timer | 任务执行时间 |
| `thread_pool_wait_time` | Timer | 任务等待时间 |

> 所有指标均带有 `thread_pool_name` 标签，用于区分不同线程池。

### 前端监控面板

前端提供三个核心页面：

| 页面 | 路由 | 功能 |
|------|------|------|
| 配置管理 | `/` | 线程池配置列表、新增、编辑、删除，支持模板快速创建 |
| 实时监控 | `/monitor` | 所有线程池实时指标总览，支持状态筛选和排序，5s 自动刷新 |
| 监控详情 | `/monitor/detail/:name` | 单个线程池趋势图（活跃线程、队列大小、等待时间），支持 5min/15min/1h/4h/1d 时间范围 |
| 日志查询 | `/log` | 配置变更日志和告警日志查询，支持告警处理 |

## Grafana 监控面板

项目提供了开箱即用的 Grafana 监控面板配置文件 `grafana-dashboard.json`，支持以下监控图表：

| 图表 | 说明 |
|------|------|
| 任务完成平均耗时 | 展示线程池任务执行的平均耗时趋势 |
| 线程池完成任务总数 | 展示已完成任务的累计数量 |
| 任务等待平均时长 | 展示任务在队列中的平均等待时间 |
| 队列占比 | 展示队列使用率（队列大小/队列容量） |
| 活跃线程占比 | 展示活跃线程占比（活跃线程数/最大线程数） |
| 拒绝任务变化值 | 展示被拒绝任务的数量变化趋势 |

### 前置条件

1. 已安装 Prometheus 并配置抓取应用指标端点（`/actuator/prometheus`）
2. 已安装 Grafana 并配置 Prometheus 数据源

### 导入步骤

#### 方式一：通过 Grafana UI 导入

1. 登录 Grafana 控制台
2. 点击左侧菜单 **Dashboards** → **New** → **Import**
3. 有两种导入方式：
   - **上传文件**：点击 **Upload dashboard JSON file**，选择 `grafana-dashboard.json` 文件
   - **粘贴内容**：将 `grafana-dashboard.json` 文件内容复制粘贴到 Import via panel json 文本框
4. 选择 Prometheus 数据源
5. 点击 **Import** 完成导入

#### 方式二：通过 API 导入

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d @grafana-dashboard.json \
  http://localhost:3000/api/dashboards/db
```

### Prometheus 配置示例

```yaml
scrape_configs:
  - job_name: 'smart-pool'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']
```

### 常用 PromQL 查询

```promql
# 活跃线程数
thread_pool_active_count

# 队列使用率
thread_pool_queue_size / thread_pool_queue_capacity * 100

# 活跃线程占比
thread_pool_active_count / thread_pool_max_pool_size * 100

# 拒绝任务数
thread_pool_reject_count

# 任务平均执行时间（秒）
rate(thread_pool_execute_time_seconds_sum[$__range]) / rate(thread_pool_execute_time_seconds_count[$__range])

# 任务平均等待时间（秒）
rate(thread_pool_wait_time_seconds_sum[$__range]) / rate(thread_pool_wait_time_seconds_count[$__range])
```

## API 接口

### 线程池配置

| 接口 | 方法 | 说明 |
|------|------|------|
| `/config/list` | GET | 获取所有线程池配置列表 |
| `/config/detail/{threadPoolName}` | GET | 获取指定线程池配置详情 |
| `/config/add` | POST | 新增线程池配置 |
| `/config/update` | PUT | 更新线程池配置 |
| `/config/delete/{threadPoolName}` | DELETE | 删除线程池配置 |
| `/config/templates` | GET | 获取配置模板列表 |
| `/config/create-from-template` | POST | 根据模板创建配置 |

### 线程池指标

| 接口 | 方法 | 说明 |
|------|------|------|
| `/metrics/latest` | GET | 获取所有线程池最新指标 |
| `/metrics/{threadPoolName}/latest` | GET | 获取指定线程池最新指标 |
| `/metrics/{threadPoolName}/timeseries` | GET | 获取指定时间范围的时间序列数据 |
| `/metrics/{threadPoolName}/timeseries/recent` | GET | 获取最近一小时时间序列数据 |
| `/metrics/{threadPoolName}/reject-trend` | GET | 获取拒绝任务趋势数据 |

### 日志查询

| 接口 | 方法 | 说明 |
|------|------|------|
| `/log/config` | GET | 查询配置变更日志（支持分页） |
| `/log/alert` | GET | 查询告警日志（支持分页） |
| `/log/alert/{id}/handle` | PUT | 标记告警为已处理 |

> 所有接口前缀为 `/admin`，完整路径如 `http://localhost:8081/admin/config/list`

## 核心设计

### 动态参数调整流程

```
Nacos 配置变更 → NacosConfigListener 接收 → ConfigChangeEvent
    → ConfigChangeHandler.handleConfigUpdate()
        → 检测变更项（ConfigChanges）
        → 增量更新线程池参数（updatePoolSizeIncrementally）
        → 更新队列容量（DynamicCapacityBlockingQueue.setCapacity）
        → 更新拒绝策略（CountingRejectedExecutionHandler）
        → 更新存活时间
        → 失败时自动回滚（rollback）
```

### 动态队列实现

`DynamicCapacityBlockingQueue` 继承 `LinkedBlockingQueue`，内部维护一个独立的 `capacity` 字段：

- 父类容量设为 `Integer.MAX_VALUE`，使 `offer/put` 不受父类容量限制
- 重写 `offer()` 方法，仅当 `size < capacity` 时允许入队
- `setCapacity()` 可运行时修改容量，无需移动队列内已有任务
- 内置等待时间统计，出队时自动记录任务在队列中的等待时长

### 任务执行监控

通过 `MonitorablePoolTask` 包装用户提交的任务，自动记录：

- **等待时间**：从任务提交入队到开始执行的时间差
- **执行时间**：任务实际执行耗时

数据通过 `TaskExecutionCallbackManager` 分发给所有注册的回调（如 Micrometer Timer、告警引擎）。

### 配置变更审计

所有配置变更均通过 `PoolLogService` 记录到 `pool_config_log` 表：

- **INIT**：应用启动时初始化线程池
- **UPDATE**：Nacos 配置变更或管理后台修改
- **DELETE**：删除线程池

记录内容包括：应用名称、线程池名称、变更前配置快照、变更后配置快照、操作人、变更来源。
