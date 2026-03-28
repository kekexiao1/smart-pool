# 动态线程池管理平台 - 前端

基于 React + Ant Design 的动态线程池配置管理前端界面。

## 功能特性

- 📋 **配置列表展示** - 查看所有线程池配置
- ➕ **新增配置** - 支持手动配置和模板快速创建
- ✏️ **编辑配置** - 修改现有线程池配置
- 🗑️ **删除配置** - 删除不再需要的配置
- 📊 **统计信息** - 显示配置总数、线程总数等统计信息
- 🎯 **模板系统** - 内置高性能、平衡型、保守型配置模板

## 技术栈

- **React 18** - 前端框架
- **Ant Design 5** - UI组件库
- **Vite** - 构建工具
- **Axios** - HTTP客户端
- **React Router** - 路由管理
- **Tailwind CSS** - 样式框架

## 快速开始

### 环境要求

- Node.js 16+
- npm 或 yarn

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

## 项目结构

```
src/
├── components/          # 公共组件
│   └── Header.jsx      # 页面头部
├── pages/              # 页面组件
│   ├── ThreadPoolConfigList.jsx    # 配置列表页面
│   └── ThreadPoolConfigForm.jsx    # 配置表单页面
├── services/           # API服务
│   └── api.js          # API接口定义
├── App.jsx             # 主应用组件
├── main.jsx            # 应用入口
└── index.css           # 全局样式
```

## API 接口

前端通过以下接口与后端通信：

- `GET /admin/config/list` - 获取配置列表
- `POST /admin/config/add` - 新增配置
- `PUT /admin/config/update` - 更新配置
- `DELETE /admin/config/delete/{name}` - 删除配置
- `GET /admin/config/templates` - 获取模板列表
- `POST /admin/config/create-from-template` - 根据模板创建配置
- `GET /admin/config/detail/{name}` - 获取配置详情

## 配置说明

### 核心参数
- **核心线程数** - 线程池保持的最小线程数量
- **最大线程数** - 线程池允许的最大线程数量
- **存活时间** - 空闲线程的存活时间
- **时间单位** - 存活时间的单位（秒、毫秒、分钟、小时）

### 队列配置
- **队列类型** - 线程池使用的队列类型
- **队列容量** - 队列的最大容量
- **拒绝策略** - 当队列满时的处理策略

### 告警配置
- **队列积压告警阈值** - 队列积压数量告警阈值
- **活跃线程占比告警阈值** - 活跃线程占比告警阈值

## 模板系统

系统内置三种配置模板：

1. **高性能模板** - 适用于高并发场景
2. **平衡型模板** - 适用于一般业务场景
3. **保守型模板** - 适用于资源敏感场景

## 开发说明

### 添加新功能

1. 在 `services/api.js` 中添加新的API接口
2. 创建对应的页面组件或修改现有组件
3. 更新路由配置（如果需要）

### 样式定制

项目使用 Tailwind CSS 和 Ant Design，可以通过修改以下文件进行样式定制：

- `src/index.css` - 全局样式
- Tailwind 配置 - `tailwind.config.js`

## 部署说明

### 开发环境

确保后端服务运行在 http://localhost:8081，前端开发服务器配置了代理。

### 生产环境

1. 构建前端应用：`npm run build`
2. 将 `dist` 目录部署到Web服务器
3. 配置反向代理指向后端API

## 注意事项

- 确保后端服务正常运行
- 配置修改会实时同步到Nacos配置中心
- 删除操作不可逆，请谨慎操作
- 建议在生产环境使用前进行充分测试