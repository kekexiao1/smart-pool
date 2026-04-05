import React, { useState, useEffect } from 'react'
import { 
  Card, 
  Table, 
  Tag, 
  Statistic, 
  Row, 
  Col, 
  Button, 
  Space, 
  message, 
  Progress,
  Badge,
  Tooltip,
  Alert,
  Select
} from 'antd'
import { 
  ReloadOutlined, 
  EyeOutlined,
  DashboardOutlined,
  ThunderboltOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  MinusOutlined
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { threadPoolMetricsAPI } from '../services/api'

function ThreadPoolMonitor() {
  const [metrics, setMetrics] = useState([])
  const [filteredMetrics, setFilteredMetrics] = useState([])
  const [loading, setLoading] = useState(false)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [rejectTrends, setRejectTrends] = useState({})
  const [lastUpdateTime, setLastUpdateTime] = useState(null)
  const [filters, setFilters] = useState({
    status: 'all'
  })
  const [sortConfig, setSortConfig] = useState({
    key: null,
    direction: 'asc' // asc, desc
  })
  const navigate = useNavigate()

  // 加载拒绝任务趋势数据
  const loadRejectTrends = async (threadPoolNames) => {
    const trends = {}
    for (const poolName of threadPoolNames) {
      try {
        const response = await threadPoolMetricsAPI.getRejectTrend(poolName, 5)
        if (response.data.code === 200) {
          trends[poolName] = response.data.data
        }
      } catch (error) {
        console.error(`获取线程池 ${poolName} 拒绝任务趋势失败:`, error)
      }
    }
    setRejectTrends(trends)
  }

  const loadMetrics = async (showSuccessMessage = false, silent = false) => {
    if (!silent) {
      setLoading(true)
    }
    try {
      const response = await threadPoolMetricsAPI.getAllLatestMetrics()
      if (response.data.code === 200) {
        const metricsData = response.data.data || []
        setMetrics(metricsData)
        
        const threadPoolNames = metricsData.map(item => item.threadPoolName)
        if (threadPoolNames.length > 0) {
          loadRejectTrends(threadPoolNames)
        }
        
        setLastUpdateTime(new Date())
        
        if (showSuccessMessage) {
          message.success(`已加载 ${metricsData.length} 个线程池指标`)
        }
      } else {
        if (!silent) {
          message.error('获取指标数据失败：' + response.data.msg)
        }
      }
    } catch (error) {
      if (!silent) {
        message.error('获取指标数据失败：' + error.message)
      }
    } finally {
      if (!silent) {
        setLoading(false)
      }
    }
  }

  useEffect(() => {
    loadMetrics(false, true)
    
    if (autoRefresh) {
      const interval = setInterval(() => loadMetrics(false, true), 5000)
      return () => clearInterval(interval)
    }
  }, [autoRefresh])

  useEffect(() => {
    const filteredData = applyFiltersAndSort(metrics)
    setFilteredMetrics(filteredData)
  }, [filters, metrics, sortConfig])

  const summaryStats = {
    total: metrics.length,
    totalActiveThreads: metrics.reduce((sum, item) => sum + (item.realTimeMetrics?.activeCount || 0), 0),
    totalQueueSize: metrics.reduce((sum, item) => sum + (item.realTimeMetrics?.queueSize || 0), 0),
    totalRejectCount: metrics.reduce((sum, item) => sum + (item.accumulateMetrics?.rejectCount || 0), 0),
    totalCompletedTasks: metrics.reduce((sum, item) => sum + (item.accumulateMetrics?.completedTaskCount || 0), 0),
    // 计算平均等待时间
    avgWaitTime: metrics.length > 0 ? 
      metrics.reduce((sum, item) => sum + (item.accumulateMetrics?.avgWaitTime || 0), 0) / metrics.length : 0,
    // 计算平均队列使用率
    avgQueueUsageRate: metrics.length > 0 ? 
      metrics.reduce((sum, item) => {
        const queueSize = item.realTimeMetrics?.queueSize || 0
        const queueRemainingCapacity = item.realTimeMetrics?.queueRemainingCapacity || 0
        const totalCapacity = queueSize + queueRemainingCapacity
        if (totalCapacity === 0) return sum
        return sum + (queueSize / totalCapacity) * 100
      }, 0) / metrics.length : 0,
    // 计算平均线程使用率
    avgThreadUsageRate: metrics.length > 0 ? 
      metrics.reduce((sum, item) => {
        const activeCount = item.realTimeMetrics?.activeCount || 0
        const currentPoolSize = item.realTimeMetrics?.currentPoolSize || 0
        if (currentPoolSize === 0) return sum
        return sum + (activeCount / currentPoolSize) * 100
      }, 0) / metrics.length : 0,
  }

  // 获取线程池状态
  const getPoolStatus = (realTimeMetrics) => {
    if (!realTimeMetrics) return 'unknown'
    
    const { activeCount, queueSize, queueRemainingCapacity } = realTimeMetrics
    
    // 队列已满且活跃线程数高 - 危险状态
    if (queueSize > 0 && queueRemainingCapacity === 0 && activeCount > 0) {
      return 'danger'
    }
    
    // 队列有积压 - 警告状态
    if (queueSize > 0) {
      return 'warning'
    }
    
    // 正常状态
    return 'success'
  }

  // 获取状态标签
  const getStatusTag = (status) => {
    const statusConfig = {
      success: { color: 'green', text: '正常' },
      warning: { color: 'orange', text: '警告' },
      danger: { color: 'red', text: '危险' },
      unknown: { color: 'gray', text: '未知' }
    }
    
    const config = statusConfig[status] || statusConfig.unknown
    return <Tag color={config.color}>{config.text}</Tag>
  }

  // 计算队列使用率
  const calculateQueueUsageRate = (queueSize, queueRemainingCapacity) => {
    const totalCapacity = queueSize + (queueRemainingCapacity || 0)
    if (totalCapacity === 0) return 0
    return (queueSize / totalCapacity) * 100
  }

  // 计算线程使用率
  const calculateThreadUsageRate = (activeCount, currentPoolSize) => {
    if (currentPoolSize === 0) return 0
    return (activeCount / currentPoolSize) * 100
  }

  const applyFiltersAndSort = (data) => {
    let filteredData = data.filter(item => {
      if (filters.status !== 'all') {
        const status = getPoolStatus(item.realTimeMetrics)
        if (filters.status !== status) {
          return false
        }
      }
      
      return true
    })

    // 排序
    if (sortConfig.key) {
      filteredData.sort((a, b) => {
        let aValue, bValue
        
        switch (sortConfig.key) {
          case 'rejectCount':
            aValue = a.accumulateMetrics?.rejectCount || 0
            bValue = b.accumulateMetrics?.rejectCount || 0
            break
          case 'queueSize':
            aValue = a.realTimeMetrics?.queueSize || 0
            bValue = b.realTimeMetrics?.queueSize || 0
            break
          case 'threadUsageRate':
            aValue = calculateThreadUsageRate(a.realTimeMetrics?.activeCount, a.realTimeMetrics?.currentPoolSize)
            bValue = calculateThreadUsageRate(b.realTimeMetrics?.activeCount, b.realTimeMetrics?.currentPoolSize)
            break
          case 'queueUsageRate':
            aValue = calculateQueueUsageRate(a.realTimeMetrics?.queueSize, a.realTimeMetrics?.queueCapacity)
            bValue = calculateQueueUsageRate(b.realTimeMetrics?.queueSize, b.realTimeMetrics?.queueCapacity)
            break
          case 'avgWaitTime':
            aValue = a.accumulateMetrics?.avgWaitTime || 0
            bValue = b.accumulateMetrics?.avgWaitTime || 0
            break
          default:
            return 0
        }
        
        if (aValue < bValue) {
          return sortConfig.direction === 'asc' ? -1 : 1
        }
        if (aValue > bValue) {
          return sortConfig.direction === 'asc' ? 1 : -1
        }
        return 0
      })
    }
    
    return filteredData
  }

  // 处理排序点击
  const handleSort = (key) => {
    let direction = 'desc' // 默认从高到低排序
    if (sortConfig.key === key && sortConfig.direction === 'desc') {
      direction = 'asc'
    }
    setSortConfig({ key, direction })
  }

  // 渲染排序图标
  const renderSortIcon = (key) => {
    if (sortConfig.key !== key) {
      return null
    }
    return sortConfig.direction === 'asc' ? '↑' : '↓'
  }

  // 渲染趋势箭头
  const renderTrendArrow = (change, changePercentage) => {
    if (change > 0) {
      return (
        <span style={{ color: '#f5222d', fontSize: '12px', marginLeft: 4 }}>
          <ArrowUpOutlined /> +{change}次（↑{Math.round(changePercentage)}%）
        </span>
      )
    } else if (change < 0) {
      return (
        <span style={{ color: '#52c41a', fontSize: '12px', marginLeft: 4 }}>
          <ArrowDownOutlined /> {change}次（↓{Math.round(Math.abs(changePercentage))}%）
        </span>
      )
    } else {
      return (
        <span style={{ color: '#666', fontSize: '12px', marginLeft: 4 }}>
          <MinusOutlined /> 0次（→0%）
        </span>
      )
    }
  }



  // 表格列定义
  const columns = [
    {
      title: (
        <span 
          style={{ cursor: 'pointer', userSelect: 'none' }}
          onClick={() => handleSort('threadPoolName')}
        >
          线程池名称 {renderSortIcon('threadPoolName')}
        </span>
      ),
      dataIndex: 'threadPoolName',
      key: 'threadPoolName',
      width: 200,
      render: (text) => (
        <Tooltip title={text}>
          <span style={{ fontWeight: 'bold', fontSize: '14px' }}>{text}</span>
        </Tooltip>
      ),
    },
    {
      title: '状态',
      key: 'status',
      width: 100,
      render: (_, record) => getStatusTag(getPoolStatus(record.realTimeMetrics)),
    },
    {
      title: '活跃线程',
      dataIndex: ['realTimeMetrics', 'activeCount'],
      key: 'activeCount',
      width: 100,
      render: (value) => <Tag color="blue">{value || 0}</Tag>,
    },
    {
      title: (
        <span 
          style={{ cursor: 'pointer', userSelect: 'none' }}
          onClick={() => handleSort('threadUsageRate')}
        >
          线程使用率 {renderSortIcon('threadUsageRate')}
        </span>
      ),
      key: 'threadUsageRate',
      width: 150,
      render: (_, record) => {
        const activeCount = record.realTimeMetrics?.activeCount || 0
        const currentPoolSize = record.realTimeMetrics?.currentPoolSize || 0
        const usageRate = calculateThreadUsageRate(activeCount, currentPoolSize)
        
        return (
          <Tooltip title={`线程使用情况: ${activeCount}/${currentPoolSize} (${usageRate.toFixed(1)}%)`}>
            <div>
              <div style={{ fontSize: '12px', color: '#666', marginBottom: 2 }}>
                {activeCount}/{currentPoolSize}
              </div>
              <Progress 
                percent={Math.round(usageRate)} 
                size="small" 
                format={(percent) => `${percent}%`}
                strokeColor={{
                  '0%': '#52c41a',
                  '50%': '#faad14',
                  '100%': '#f5222d',
                }}
              />
            </div>
          </Tooltip>
        )
      },
    },
    {
      title: (
        <span 
          style={{ cursor: 'pointer', userSelect: 'none' }}
          onClick={() => handleSort('queueUsageRate')}
        >
          队列使用率 {renderSortIcon('queueUsageRate')}
        </span>
      ),
      key: 'queueUsageRate',
      width: 150,
      render: (_, record) => {
        const queueSize = record.realTimeMetrics?.queueSize || 0
        const queueRemainingCapacity = record.realTimeMetrics?.queueRemainingCapacity || 0
        const usageRate = calculateQueueUsageRate(queueSize, queueRemainingCapacity)
        const totalCapacity = queueSize + queueRemainingCapacity
        
        return (
          <Tooltip title={`队列使用情况: ${queueSize}/${totalCapacity} (${usageRate.toFixed(1)}%)`}>
            <div>
              <div style={{ fontSize: '12px', color: '#666', marginBottom: 2 }}>
                {queueSize}/{totalCapacity}
              </div>
              <Progress 
                percent={Math.round(usageRate)} 
                size="small" 
                format={(percent) => `${percent}%`}
                strokeColor={{
                  '0%': '#52c41a',
                  '50%': '#faad14',
                  '100%': '#f5222d',
                }}
              />
            </div>
          </Tooltip>
        )
      },
    },
    {
      title: (
        <span 
          style={{ cursor: 'pointer', userSelect: 'none' }}
          onClick={() => handleSort('rejectCount')}
        >
          拒绝任务数 {renderSortIcon('rejectCount')}
        </span>
      ),
      dataIndex: ['accumulateMetrics', 'rejectCount'],
      key: 'rejectCount',
      width: 220,
      render: (value, record) => {
        const trend = rejectTrends[record.threadPoolName]
        return (
          <Tooltip title={
            trend ? 
              `累计拒绝任务数: ${value || 0}\n较5分钟前: ${trend.change >= 0 ? '+' : ''}${trend.change}次 (${trend.change >= 0 ? '↑' : '↓'}${Math.round(Math.abs(trend.changePercentage))}%)` :
              `累计拒绝任务数: ${value || 0}`
          }>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Badge 
                  count={value || 0} 
                  showZero 
                  style={{ backgroundColor: value > 0 ? '#f50' : '#52c41a' }}
                />
              </div>
              {trend && trend.change !== 0 && (
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: 4,
                  fontSize: '11px',
                  color: trend.change > 0 ? '#f5222d' : '#52c41a',
                  background: trend.change > 0 ? '#fff2f0' : '#f6ffed',
                  padding: '2px 6px',
                  borderRadius: 4,
                  border: `1px solid ${trend.change > 0 ? '#ffccc7' : '#b7eb8f'}`
                }}>
                  <span style={{ fontWeight: 'bold' }}>
                    较上5分钟
                  </span>
                  <span style={{ fontWeight: 'bold' }}>
                    {trend.change > 0 ? '+' : ''}{trend.change}次
                  </span>
                  <span style={{ color: trend.change > 0 ? '#f5222d' : '#52c41a' }}>
                    ({trend.change >= 0 ? '↑' : '↓'}{Math.round(Math.abs(trend.changePercentage))}%)
                  </span>
                </div>
              )}
            </div>
          </Tooltip>
        )
      },
    },
    {
      title: '完成任务数',
      dataIndex: ['accumulateMetrics', 'completedTaskCount'],
      key: 'completedTaskCount',
      width: 120,
      render: (value) => (
        <Tooltip title={`累计完成任务数: ${value || 0}`}>
          <span style={{ color: '#1890ff', fontWeight: 'bold' }}>
            {value ? (value > 1000000 ? `${(value / 1000000).toFixed(1)}M` : value.toLocaleString()) : 0}
          </span>
        </Tooltip>
      ),
    },
    {
      title: (
        <span 
          style={{ cursor: 'pointer', userSelect: 'none' }}
          onClick={() => handleSort('avgWaitTime')}
        >
          平均等待时间 {renderSortIcon('avgWaitTime')}
        </span>
      ),
      dataIndex: ['accumulateMetrics', 'avgWaitTime'],
      key: 'avgWaitTime',
      width: 140,
      render: (value) => (
        <Tooltip title={`任务平均等待时间: ${value || 0}ms`}>
          <span style={{ 
            color: value > 1000 ? '#f5222d' : value > 500 ? '#faad14' : '#52c41a',
            fontWeight: 'bold',
            fontSize: '13px'
          }}>
            {value ? `${value}ms` : '0ms'}
          </span>
        </Tooltip>
      ),
    },
    {
      title: '最后更新时间',
      dataIndex: 'timestamp',
      key: 'timestamp',
      width: 180,
      render: (timestamp) => (
        <Tooltip title={timestamp}>
          <span style={{ color: '#666', fontSize: '12px' }}>
            {timestamp ? new Date(timestamp).toLocaleString() : '-'}
          </span>
        </Tooltip>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="查看详情">
            <Button 
              type="link" 
              icon={<EyeOutlined />} 
              size="small"
              onClick={() => navigate(`/monitor/detail/${record.threadPoolName}`)}
            />
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <div>
      {/* 统计卡片 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={3}>
          <Card>
            <Statistic
              title="监控线程池"
              value={summaryStats.total}
              prefix={<DashboardOutlined />}
              suffix="个"
            />
          </Card>
        </Col>
        <Col span={3}>
          <Card>
            <Statistic
              title="活跃线程总数"
              value={summaryStats.totalActiveThreads}
              prefix={<ThunderboltOutlined />}
              suffix="个"
            />
          </Card>
        </Col>
        <Col span={3}>
          <Card>
            <Statistic
              title="平均线程使用率"
              value={Math.round(summaryStats.avgThreadUsageRate)}
              prefix={<ThunderboltOutlined />}
              suffix="%"
              valueStyle={{ color: summaryStats.avgThreadUsageRate > 80 ? '#f5222d' : summaryStats.avgThreadUsageRate > 50 ? '#faad14' : '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={3}>
          <Card>
            <Statistic
              title="平均队列使用率"
              value={Math.round(summaryStats.avgQueueUsageRate)}
              prefix={<ExclamationCircleOutlined />}
              suffix="%"
              valueStyle={{ color: summaryStats.avgQueueUsageRate > 80 ? '#f5222d' : summaryStats.avgQueueUsageRate > 50 ? '#faad14' : '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={3}>
          <Card>
            <Statistic
              title="队列积压总数"
              value={summaryStats.totalQueueSize}
              prefix={<ExclamationCircleOutlined />}
              suffix="个"
            />
          </Card>
        </Col>
        <Col span={3}>
          <Card>
            <Statistic
              title="拒绝任务总数"
              value={summaryStats.totalRejectCount}
              prefix={<ClockCircleOutlined />}
              suffix="次"
            />
          </Card>
        </Col>
        <Col span={3}>
          <Card>
            <Statistic
              title="平均等待时间"
              value={Math.round(summaryStats.avgWaitTime)}
              prefix={<ClockCircleOutlined />}
              suffix="ms"
              valueStyle={{ color: summaryStats.avgWaitTime > 1000 ? '#f5222d' : summaryStats.avgWaitTime > 500 ? '#faad14' : '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 状态说明 */}
      <Alert
        message="线程池监控说明"
        description={
          <div>
            <div style={{ marginBottom: 8 }}>
              <strong>状态说明：</strong>
              <Tag color="green">正常</Tag> - 队列无积压，运行正常
              <Tag color="orange" style={{ marginLeft: 8 }}>警告</Tag> - 队列有积压，需关注
              <Tag color="red" style={{ marginLeft: 8 }}>危险</Tag> - 队列已满，可能影响性能
              <Tag color="gray" style={{ marginLeft: 8 }}>未知</Tag> - 无法获取状态信息
            </div>
            <div>
              <strong>使用率说明：</strong>
              <Tag color="green">低负载</Tag> - 使用率 ≤ 50%
              <Tag color="orange" style={{ marginLeft: 8 }}>中等负载</Tag> - 使用率 50% - 80%
              <Tag color="red" style={{ marginLeft: 8 }}>高负载</Tag> - 使用率 &gt; 80%
            </div>
          </div>
        }
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />

      {/* 筛选器 */}
      <Card style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontWeight: 'bold' }}>快速筛选：</span>
            <Select
              value={filters.status}
              onChange={(value) => setFilters({ ...filters, status: value })}
              style={{ width: 120 }}
              placeholder="状态筛选"
            >
              <Select.Option value="all">全部状态</Select.Option>
              <Select.Option value="success">正常</Select.Option>
              <Select.Option value="warning">警告</Select.Option>
              <Select.Option value="danger">危险</Select.Option>
            </Select>
          </div>
          
          <div style={{ flex: 1 }}></div>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            {lastUpdateTime && (
              <div style={{ fontSize: '12px', color: '#666' }}>
                <ClockCircleOutlined style={{ marginRight: 4 }} />
                最后更新: {lastUpdateTime.toLocaleTimeString()}
              </div>
            )}
            <div style={{ fontSize: '12px', color: '#666' }}>
              显示 {filteredMetrics.length} / {metrics.length} 个线程池
            </div>
          </div>
        </div>
      </Card>

      {/* 监控表格 */}
      <Card 
        className="monitor-card"
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>
              <DashboardOutlined style={{ marginRight: 8 }} />
              线程池实时监控
            </span>
            <Space>
              <Button 
                type={autoRefresh ? 'primary' : 'default'}
                icon={<ReloadOutlined />} 
                onClick={() => setAutoRefresh(!autoRefresh)}
              >
                {autoRefresh ? '停止自动刷新' : '开启自动刷新'}
              </Button>
              <Button 
                icon={<ReloadOutlined />} 
                onClick={() => loadMetrics(true)}
                loading={loading}
              >
                手动刷新
              </Button>
            </Space>
          </div>
        }
      >
        <Table
          columns={columns}
          dataSource={filteredMetrics}
          rowKey="threadPoolName"
          loading={loading}
          scroll={{ x: 1200 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`
          }}
        />
      </Card>
    </div>
  )
}

export default ThreadPoolMonitor