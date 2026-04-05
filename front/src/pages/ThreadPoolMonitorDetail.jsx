import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Row, Col, Button, Space, message, Spin, Statistic, Tag, Alert, Radio } from 'antd'
import { ArrowLeftOutlined, ReloadOutlined, ThunderboltOutlined, ExclamationCircleOutlined, ClockCircleOutlined } from '@ant-design/icons'
import { Line } from '@ant-design/charts'
import { threadPoolMetricsAPI } from '../services/api'

function ThreadPoolMonitorDetail() {
  const { threadPoolName } = useParams()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [timeSeriesData, setTimeSeriesData] = useState([])
  const [latestMetrics, setLatestMetrics] = useState(null)
  const [error, setError] = useState(null)
  const [timeRange, setTimeRange] = useState(60)
  const [autoRefresh, setAutoRefresh] = useState(true)
  const [lastUpdateTime, setLastUpdateTime] = useState(null)

  const timeRangeOptions = [
    { label: '5分钟', value: 5 },
    { label: '15分钟', value: 15 },
    { label: '1小时', value: 60 },
    { label: '4小时', value: 240 },
    { label: '1天', value: 1440 },
  ]

  const loadData = async (showMessage = false, silent = false) => {
    if (!silent) {
      setLoading(true)
    }
    setError(null)
    
    try {
      let timeSeriesResponse
      
      if (timeRange === 60) {
        console.log('使用 recent 接口获取最近1小时数据')
        timeSeriesResponse = await threadPoolMetricsAPI.getRecentTimeSeriesData(threadPoolName)
      } else {
        const endTime = new Date()
        const startTime = new Date(endTime.getTime() - timeRange * 60 * 1000)
        
        const formatDateTime = (date) => {
          const year = date.getFullYear()
          const month = String(date.getMonth() + 1).padStart(2, '0')
          const day = String(date.getDate()).padStart(2, '0')
          const hours = String(date.getHours()).padStart(2, '0')
          const minutes = String(date.getMinutes()).padStart(2, '0')
          const seconds = String(date.getSeconds()).padStart(2, '0')
          return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
        }
        
        const startTimeStr = formatDateTime(startTime)
        const endTimeStr = formatDateTime(endTime)
        
        console.log('请求时间范围:', {
          startTime: startTimeStr,
          endTime: endTimeStr,
          timeRange
        })
        
        timeSeriesResponse = await threadPoolMetricsAPI.getTimeSeriesData(
          threadPoolName,
          startTimeStr,
          endTimeStr
        )
      }
      
      const latestResponse = await threadPoolMetricsAPI.getLatestMetrics(threadPoolName)

      console.log('时间序列数据响应:', timeSeriesResponse.data)
      console.log('最新指标响应:', latestResponse.data)

      if (timeSeriesResponse.data.code === 200) {
        console.log('时间序列数据:', timeSeriesResponse.data.data)
        setTimeSeriesData(timeSeriesResponse.data.data || [])
      } else {
        const errorMsg = '获取时间序列数据失败：' + timeSeriesResponse.data.msg
        setError(errorMsg)
        if (!silent) {
          message.error(errorMsg)
        }
      }

      if (latestResponse.data.code === 200) {
        setLatestMetrics(latestResponse.data.data)
      } else {
        if (!error) {
          const errorMsg = '获取最新指标失败：' + latestResponse.data.msg
          setError(errorMsg)
          if (!silent) {
            message.error(errorMsg)
          }
        }
      }

      setLastUpdateTime(new Date())

      if (showMessage && !error) {
        message.success('数据刷新成功')
      }
    } catch (error) {
      console.error('获取数据失败:', error)
      const errorMsg = '获取数据失败：' + error.message
      setError(errorMsg)
      if (!silent) {
        message.error(errorMsg)
      }
    } finally {
      if (!silent) {
        setLoading(false)
      }
    }
  }

  useEffect(() => {
    loadData(false, true)
    
    if (autoRefresh) {
      const interval = setInterval(() => loadData(false, true), 5000)
      return () => clearInterval(interval)
    }
  }, [threadPoolName, timeRange, autoRefresh])

  const handleTimeRangeChange = (e) => {
    setTimeRange(e.target.value)
  }

  const formatTime = (timestamp) => {
    const date = new Date(timestamp)
    
    if (timeRange <= 15) {
      return date.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit',
        second: '2-digit'
      })
    } else if (timeRange <= 240) {
      return date.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit'
      })
    } else {
      return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
    }
  }

  const prepareActiveThreadData = () => {
    return timeSeriesData.map(item => ({
      time: formatTime(item.timestamp),
      value: item.realTimeMetrics?.activeCount || 0,
      poolSize: item.realTimeMetrics?.currentPoolSize || 0
    }))
  }

  const prepareQueueData = () => {
    return timeSeriesData.map(item => ({
      time: formatTime(item.timestamp),
      value: item.realTimeMetrics?.queueSize || 0,
      capacity: (item.realTimeMetrics?.queueSize || 0) + (item.realTimeMetrics?.queueRemainingCapacity || 0)
    }))
  }

  const prepareWaitTimeData = () => {
    return timeSeriesData.map(item => ({
      time: formatTime(item.timestamp),
      value: item.accumulateMetrics?.avgWaitTime || 0
    }))
  }

  const getXAxisConfig = () => {
    let tickCount = 10
    
    if (timeRange <= 5) {
      tickCount = 12
    } else if (timeRange <= 15) {
      tickCount = 18
    } else if (timeRange <= 60) {
      tickCount = 12
    } else if (timeRange <= 240) {
      tickCount = 24
    } else {
      tickCount = 24
    }
    
    const actualDataCount = timeSeriesData.length
    if (actualDataCount > 0 && actualDataCount < tickCount) {
      tickCount = actualDataCount
    }
    
    return {
      title: {
        text: '时间',
        style: {
          fontSize: 12,
          fill: '#8c8c8c',
        }
      },
      label: {
        style: {
          fill: '#8c8c8c',
        },
        rotate: -45,
        autoHide: true,
        autoRotate: true
      },
      tickCount: tickCount
    }
  }

  const activeThreadConfig = {
    data: prepareActiveThreadData(),
    xField: 'time',
    yField: 'value',
    smooth: true,
    animation: {
      appear: {
        animation: 'path-in',
        duration: 1000,
      },
    },
    point: false,
    tooltip: { 
      showTitle: true, 
      showMarkers: true, 
      shared: true, 
      follow: true,
      customContent: (title, items) => {
        if (!items || items.length === 0) return '';
        const item = items[0];
        const datum = item.data || {};
        const activeThreads = datum.value || 0;
        const poolSize = datum.poolSize || 0;
        
        const usageRate = poolSize > 0 ? (activeThreads / poolSize) * 100 : 0; 
        const usageColor = usageRate > 80 ? '#f5222d' : usageRate > 50 ? '#faad14' : '#52c41a';

        return `
            <div style="padding: 16px; background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.12); min-width: 220px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;"> 
              
              <div style="font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 12px; border-bottom: 1px solid #f0f0f0; padding-bottom: 8px;"> 
                ⏰ ${datum.time || title}
              </div> 
              
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;"> 
                <div style="display: flex; align-items: center;"> 
                  <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #1890ff; margin-right: 8px;"></span> 
                  <span style="color: #595959; font-size: 13px;">活跃线程</span> 
                </div> 
                <span style="font-weight: 600; color: #1890ff; font-size: 14px;">${activeThreads}</span> 
              </div> 
              
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;"> 
                <div style="display: flex; align-items: center;"> 
                  <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #52c41a; margin-right: 8px;"></span> 
                  <span style="color: #595959; font-size: 13px;">线程池大小</span> 
                </div> 
                <span style="font-weight: 600; color: #52c41a; font-size: 14px;">${poolSize}</span> 
              </div> 
              
              <div style="height: 1px; background: #f0f0f0; margin: 12px 0;"></div> 
              
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;"> 
                <span style="color: #595959; font-size: 13px; font-weight: 500;">线程使用率</span>
                <span style="font-weight: 700; color: ${usageColor}; font-size: 15px;"> 
                  ${usageRate.toFixed(1)}% 
                </span> 
              </div> 
              
              <div style="background: #f5f5f5; border-radius: 4px; height: 6px; overflow: hidden;"> 
                <div style="height: 100%; background: ${usageColor}; width: ${Math.min(usageRate, 100)}%; transition: width 0.3s ease-in-out;"></div> 
              </div> 
              
            </div>
          `;
      }
    },
    yAxis: {
      min: 0,
      title: {
        text: '线程数',
        style: {
          fontSize: 12,
          fill: '#8c8c8c',
        }
      },
      grid: {
        line: {
          style: {
            stroke: '#f0f0f0',
            lineWidth: 1,
          }
        }
      },
      label: {
        style: {
          fill: '#8c8c8c',
        }
      }
    },
    xAxis: getXAxisConfig(),
    lineStyle: {
      lineWidth: 3,
      stroke: '#1890ff',
    },
    color: '#1890ff',
    areaStyle: {
      fill: 'l(270) 0:#ffffff 0.5:#d6e4ff 1:#1890ff',
      fillOpacity: 0.3,
    },
  }

  const queueConfig = {
    data: prepareQueueData(),
    xField: 'time',
    yField: 'value',
    smooth: true,
    animation: {
      appear: {
        animation: 'path-in',
        duration: 1000,
      },
    },
    point: false,
    tooltip: { 
      showTitle: true, 
      showMarkers: true, 
      shared: true, 
      follow: true,
      customContent: (title, items) => {
        if (!items || items.length === 0) return '';
        const item = items[0];
        const datum = item.data || {};
        const queueSize = datum.value || 0;
        const queueCapacity = datum.capacity || 0; 
        const remainingCapacity = queueCapacity - queueSize;
        
        const usageRate = queueCapacity > 0 ? (queueSize / queueCapacity) * 100 : 0; 
        const usageColor = usageRate > 80 ? '#f5222d' : usageRate > 50 ? '#faad14' : '#52c41a';

        return `
            <div style="padding: 16px; background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.12); min-width: 220px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;"> 
              
              <div style="font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 12px; border-bottom: 1px solid #f0f0f0; padding-bottom: 8px;"> 
                ⏰ ${datum.time || title}
              </div> 
              
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;"> 
                <div style="display: flex; align-items: center;"> 
                  <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #1890ff; margin-right: 8px;"></span> 
                  <span style="color: #595959; font-size: 13px;">队列大小</span> 
                </div> 
                <span style="font-weight: 600; color: #1890ff; font-size: 14px;">${queueSize}</span> 
              </div> 
              
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;"> 
                <div style="display: flex; align-items: center;"> 
                  <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #52c41a; margin-right: 8px;"></span> 
                  <span style="color: #595959; font-size: 13px;">队列容量</span> 
                </div> 
                <span style="font-weight: 600; color: #52c41a; font-size: 14px;">${queueCapacity}</span> 
              </div> 
              
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;"> 
                <div style="display: flex; align-items: center;"> 
                  <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #faad14; margin-right: 8px;"></span> 
                  <span style="color: #595959; font-size: 13px;">剩余容量</span> 
                </div> 
                <span style="font-weight: 600; color: #faad14; font-size: 14px;">${remainingCapacity}</span> 
              </div> 
              
              <div style="height: 1px; background: #f0f0f0; margin: 12px 0;"></div> 
              
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;"> 
                <span style="color: #595959; font-size: 13px; font-weight: 500;">队列使用率</span>
                <span style="font-weight: 700; color: ${usageColor}; font-size: 15px;"> 
                  ${usageRate.toFixed(1)}% 
                </span> 
              </div> 
              
              <div style="background: #f5f5f5; border-radius: 4px; height: 6px; overflow: hidden;"> 
                <div style="height: 100%; background: ${usageColor}; width: ${Math.min(usageRate, 100)}%; transition: width 0.3s ease-in-out;"></div> 
              </div> 
              
            </div>
          `;
      }
    },
    yAxis: {
      min: 0,
      title: {
        text: '队列大小',
        style: {
          fontSize: 12,
          fill: '#8c8c8c',
        }
      },
      grid: {
        line: {
          style: {
            stroke: '#f0f0f0',
            lineWidth: 1,
          }
        }
      },
      label: {
        style: {
          fill: '#8c8c8c',
        }
      }
    },
    xAxis: getXAxisConfig(),
    lineStyle: {
      lineWidth: 3,
      stroke: '#1890ff',
    },
    color: '#1890ff',
    areaStyle: {
      fill: 'l(270) 0:#ffffff 0.5:#d6e4ff 1:#1890ff',
      fillOpacity: 0.3,
    },
  }

  const waitTimeConfig = {
    data: prepareWaitTimeData(),
    xField: 'time',
    yField: 'value',
    smooth: true,
    animation: {
      appear: {
        animation: 'path-in',
        duration: 1000,
      },
    },
    point: false,
    tooltip: { 
      showTitle: true, 
      showMarkers: true, 
      shared: true, 
      follow: true,
      customContent: (title, items) => {
        if (!items || items.length === 0) return '';
        const item = items[0];
        const datum = item.data || {};
        const waitTime = datum.value || 0;
        const statusColor = waitTime > 1000 ? '#f5222d' : waitTime > 500 ? '#faad14' : '#52c41a';

        return `
            <div style="padding: 16px; background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.12); min-width: 220px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;"> 
              
              <div style="font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 12px; border-bottom: 1px solid #f0f0f0; padding-bottom: 8px;"> 
                ⏰ ${datum.time || title}
              </div> 
              
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;"> 
                <div style="display: flex; align-items: center;"> 
                  <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${statusColor}; margin-right: 8px;"></span> 
                  <span style="color: #595959; font-size: 13px;">平均等待时间</span> 
                </div> 
                <span style="font-weight: 600; color: ${statusColor}; font-size: 14px;">${waitTime}ms</span> 
              </div> 
              
              <div style="height: 1px; background: #f0f0f0; margin: 12px 0;"></div> 
              
              <div style="color: #8c8c8c; font-size: 12px; line-height: 1.4;">
                <div>• ${waitTime > 1000 ? '⚠️ 等待时间过长' : waitTime > 500 ? '⚠️ 等待时间偏高' : '✅ 等待时间正常'}</div>
                <div>• 建议阈值: &lt;500ms</div>
              </div>
              
            </div>
          `;
      }
    },
    yAxis: {
      min: 0,
      title: {
        text: '等待时间(ms)',
        style: {
          fontSize: 12,
          fill: '#8c8c8c',
        }
      },
      grid: {
        line: {
          style: {
            stroke: '#f0f0f0',
            lineWidth: 1,
          }
        }
      },
      label: {
        style: {
          fill: '#8c8c8c',
        }
      }
    },
    xAxis: getXAxisConfig(),
    lineStyle: {
      lineWidth: 3,
      stroke: '#1890ff',
    },
    color: '#1890ff',
    areaStyle: {
      fill: 'l(270) 0:#ffffff 0.5:#d6e4ff 1:#1890ff',
      fillOpacity: 0.3,
    },
  }

  return (
    <div>
      <Card
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Space>
              <Button 
                icon={<ArrowLeftOutlined />} 
                onClick={() => navigate('/monitor')}
              >
                返回监控列表
              </Button>
              <span style={{ fontSize: '18px', fontWeight: 'bold' }}>
                {threadPoolName} - 监控详情
              </span>
            </Space>
            <Space>
              {lastUpdateTime && (
                <span style={{ fontSize: '12px', color: '#666' }}>
                  <ClockCircleOutlined style={{ marginRight: 4 }} />
                  最后更新: {lastUpdateTime.toLocaleTimeString()}
                </span>
              )}
              <Radio.Group
                value={timeRange}
                onChange={handleTimeRangeChange}
                buttonStyle="solid"
                size="small"
              >
                {timeRangeOptions.map(option => (
                  <Radio.Button key={option.value} value={option.value}>
                    {option.label}
                  </Radio.Button>
                ))}
              </Radio.Group>
              <Button 
                type={autoRefresh ? 'primary' : 'default'}
                icon={<ReloadOutlined />} 
                onClick={() => setAutoRefresh(!autoRefresh)}
              >
                {autoRefresh ? '停止自动刷新' : '开启自动刷新'}
              </Button>
              <Button 
                icon={<ReloadOutlined />} 
                onClick={() => loadData(true)}
                loading={loading}
              >
                手动刷新
              </Button>
            </Space>
          </div>
        }
      >
        {error && (
          <Alert
            message="数据加载错误"
            description={error}
            type="error"
            closable
            onClose={() => setError(null)}
            style={{ marginBottom: 16 }}
          />
        )}

        {latestMetrics && (
          <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
            <Col span={3}>
              <Card hoverable>
                <Statistic
                  title="当前活跃线程"
                  value={latestMetrics.realTimeMetrics?.activeCount || 0}
                  prefix={<ThunderboltOutlined />}
                  suffix={`/ ${latestMetrics.realTimeMetrics?.currentPoolSize || 0}`}
                />
              </Card>
            </Col>
            <Col span={3}>
              <Card hoverable>
                <Statistic
                  title="线程使用率"
                  value={latestMetrics.realTimeMetrics?.currentPoolSize > 0 ? 
                    ((latestMetrics.realTimeMetrics?.activeCount / latestMetrics.realTimeMetrics?.currentPoolSize) * 100).toFixed(1) : 0}
                  suffix="%"
                  valueStyle={{ 
                    color: (latestMetrics.realTimeMetrics?.activeCount / latestMetrics.realTimeMetrics?.currentPoolSize) > 0.8 ? '#f5222d' : 
                           (latestMetrics.realTimeMetrics?.activeCount / latestMetrics.realTimeMetrics?.currentPoolSize) > 0.5 ? '#faad14' : '#52c41a'
                  }}
                />
              </Card>
            </Col>
            <Col span={3}>
              <Card hoverable>
                <Statistic
                  title="队列积压"
                  value={latestMetrics.realTimeMetrics?.queueSize || 0}
                  prefix={<ExclamationCircleOutlined />}
                  suffix={`/ ${(latestMetrics.realTimeMetrics?.queueSize || 0) + (latestMetrics.realTimeMetrics?.queueRemainingCapacity || 0)}`}
                />
              </Card>
            </Col>
            <Col span={3}>
              <Card hoverable>
                <Statistic
                  title="队列使用率"
                  value={
                    ((latestMetrics.realTimeMetrics?.queueSize || 0) + (latestMetrics.realTimeMetrics?.queueRemainingCapacity || 0)) > 0 ?
                    ((latestMetrics.realTimeMetrics?.queueSize / 
                    ((latestMetrics.realTimeMetrics?.queueSize || 0) + (latestMetrics.realTimeMetrics?.queueRemainingCapacity || 0))) * 100).toFixed(1) : 0
                  }
                  suffix="%"
                  valueStyle={{ 
                    color: latestMetrics.realTimeMetrics?.queueSize > 
                           ((latestMetrics.realTimeMetrics?.queueSize || 0) + (latestMetrics.realTimeMetrics?.queueRemainingCapacity || 0)) * 0.8 ? '#f5222d' : 
                           latestMetrics.realTimeMetrics?.queueSize > 
                           ((latestMetrics.realTimeMetrics?.queueSize || 0) + (latestMetrics.realTimeMetrics?.queueRemainingCapacity || 0)) * 0.5 ? '#faad14' : '#52c41a'
                  }}
                />
              </Card>
            </Col>
            <Col span={3}>
              <Card hoverable>
                <Statistic
                  title="累计拒绝任务"
                  value={latestMetrics.accumulateMetrics?.rejectCount || 0}
                  valueStyle={{ color: latestMetrics.accumulateMetrics?.rejectCount > 0 ? '#f5222d' : '#52c41a' }}
                />
              </Card>
            </Col>
            <Col span={3}>
              <Card hoverable>
                <Statistic
                  title="累计完成任务"
                  value={latestMetrics.accumulateMetrics?.completedTaskCount || 0}
                  valueStyle={{ color: '#1890ff' }}
                />
              </Card>
            </Col>
            <Col span={3}>
              <Card hoverable>
                <Statistic
                  title="平均等待时间"
                  value={latestMetrics.accumulateMetrics?.avgWaitTime || 0}
                  suffix="ms"
                  valueStyle={{ 
                    color: latestMetrics.accumulateMetrics?.avgWaitTime > 1000 ? '#f5222d' : 
                           latestMetrics.accumulateMetrics?.avgWaitTime > 500 ? '#faad14' : '#52c41a'
                  }}
                />
              </Card>
            </Col>
          </Row>
        )}

        <Spin spinning={loading} tip="正在加载数据...">
          <Row gutter={[16, 16]}>
            <Col span={24}>
              <Card 
                title={
                  <Space>
                    <ThunderboltOutlined style={{ color: '#1890ff', fontSize: '18px' }} />
                    <span style={{ fontSize: '16px', fontWeight: 'bold' }}>活跃线程数变化趋势</span>
                    <Tag color="blue" icon={<ClockCircleOutlined />}>
                      最近 {timeRangeOptions.find(o => o.value === timeRange)?.label}
                    </Tag>
                  </Space>
                }
                hoverable
              >
                {timeSeriesData.length > 0 ? (
                  <Line {...activeThreadConfig} height={350} />
                ) : (
                  <div style={{ textAlign: 'center', padding: '100px 0', color: '#999' }}>
                    {loading ? '正在加载数据...' : '暂无数据'}
                  </div>
                )}
              </Card>
            </Col>

            <Col span={24}>
              <Card 
                title={
                  <Space>
                    <ExclamationCircleOutlined style={{ color: '#1890ff', fontSize: '18px' }} />
                    <span style={{ fontSize: '16px', fontWeight: 'bold' }}>队列积压数变化趋势</span>
                    <Tag color="blue" icon={<ClockCircleOutlined />}>
                      最近 {timeRangeOptions.find(o => o.value === timeRange)?.label}
                    </Tag>
                  </Space>
                }
                hoverable
              >
                {timeSeriesData.length > 0 ? (
                  <Line {...queueConfig} height={350} />
                ) : (
                  <div style={{ textAlign: 'center', padding: '100px 0', color: '#999' }}>
                    {loading ? '正在加载数据...' : '暂无数据'}
                  </div>
                )}
              </Card>
            </Col>

            <Col span={24}>
              <Card 
                title={
                  <Space>
                    <ClockCircleOutlined style={{ color: '#1890ff', fontSize: '18px' }} />
                    <span style={{ fontSize: '16px', fontWeight: 'bold' }}>平均等待时间变化趋势</span>
                    <Tag color="blue" icon={<ClockCircleOutlined />}>
                      最近 {timeRangeOptions.find(o => o.value === timeRange)?.label}
                    </Tag>
                  </Space>
                }
                hoverable
              >
                {timeSeriesData.length > 0 ? (
                  <Line {...waitTimeConfig} height={350} />
                ) : (
                  <div style={{ textAlign: 'center', padding: '100px 0', color: '#999' }}>
                    {loading ? '正在加载数据...' : '暂无数据'}
                  </div>
                )}
              </Card>
            </Col>
          </Row>
        </Spin>
      </Card>
    </div>
  )
}

export default ThreadPoolMonitorDetail
