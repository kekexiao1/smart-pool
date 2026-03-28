import React, { useState, useEffect } from 'react'
import { 
  Table, 
  Card, 
  Button, 
  Space, 
  Tag, 
  message, 
  Popconfirm, 
  Tooltip,
  Row,
  Col,
  Statistic,
  Badge
} from 'antd'
import { 
  EditOutlined, 
  DeleteOutlined, 
  EyeOutlined, 
  ReloadOutlined,
  PlusOutlined 
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { threadPoolConfigAPI } from '../services/api'

function ThreadPoolConfigList() {
  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const loadConfigs = async () => {
    setLoading(true)
    try {
      const response = await threadPoolConfigAPI.getConfigList()
      if (response.data.code === 200) {
        setConfigs(response.data.data || [])
      } else {
        message.error('获取配置列表失败：' + response.data.msg)
      }
    } catch (error) {
      message.error('获取配置列表失败：' + error.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadConfigs()
  }, [])

  const handleDelete = async (threadPoolName) => {
    try {
      const response = await threadPoolConfigAPI.deleteConfig(threadPoolName)
      if (response.data.code === 200) {
        message.success('删除配置成功')
        loadConfigs()
      } else {
        message.error('删除配置失败：' + response.data.msg)
      }
    } catch (error) {
      message.error('删除配置失败：' + error.message)
    }
  }

  const columns = [
    {
      title: '线程池名称',
      dataIndex: 'threadPoolName',
      key: 'threadPoolName',
      width: 200,
      render: (text, record) => (
        <Tooltip title={record.config?.threadPoolName || text}>
          <span style={{ fontWeight: 'bold' }}>{text}</span>
        </Tooltip>
      ),
    },
    {
      title: '核心线程数',
      dataIndex: ['config', 'corePoolSize'],
      key: 'corePoolSize',
      width: 100,
      render: (value) => <Tag color="blue">{value}</Tag>,
    },
    {
      title: '最大线程数',
      dataIndex: ['config', 'maximumPoolSize'],
      key: 'maximumPoolSize',
      width: 100,
      render: (value) => <Tag color="green">{value}</Tag>,
    },
    {
      title: '队列容量',
      dataIndex: ['config', 'queueCapacity'],
      key: 'queueCapacity',
      width: 100,
      render: (value) => <Tag color="orange">{value}</Tag>,
    },
    {
      title: '存活时间',
      dataIndex: ['config', 'keepAliveTime'],
      key: 'keepAliveTime',
      width: 120,
      render: (value, record) => {
        const unit = record.config?.unit || 'SECONDS'
        let unitText = 's'
        if (unit === 'MILLISECONDS') unitText = 'ms'
        else if (unit === 'MINUTES') unitText = 'm'
        else if (unit === 'HOURS') unitText = 'h'
        return <span>{value}{unitText}</span>
      },
    },
    {
      title: '拒绝策略',
      dataIndex: ['config', 'rejectedHandlerClass'],
      key: 'rejectedHandlerClass',
      width: 120,
      render: (value) => <Tag color="purple">{value}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="查看详情">
            <Button 
              type="link" 
              icon={<EyeOutlined />} 
              size="small"
              onClick={() => navigate(`/config/edit/${record.threadPoolName}`)}
            />
          </Tooltip>
          <Tooltip title="编辑配置">
            <Button 
              type="link" 
              icon={<EditOutlined />} 
              size="small"
              onClick={() => navigate(`/config/edit/${record.threadPoolName}`)}
            />
          </Tooltip>
          <Popconfirm
            title="确定删除这个配置吗？"
            description="删除后无法恢复"
            onConfirm={() => handleDelete(record.threadPoolName)}
            okText="确定"
            cancelText="取消"
          >
            <Tooltip title="删除配置">
              <Button 
                type="link" 
                danger 
                icon={<DeleteOutlined />} 
                size="small" 
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const summaryStats = {
    total: configs.length,
    totalCoreThreads: configs.reduce((sum, item) => sum + (item.config?.corePoolSize || 0), 0),
    totalMaxThreads: configs.reduce((sum, item) => sum + (item.config?.maximumPoolSize || 0), 0),
  }

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总配置数"
              value={summaryStats.total}
              prefix={<Badge status="processing" />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="核心线程总数"
              value={summaryStats.totalCoreThreads}
              suffix="个"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="最大线程总数"
              value={summaryStats.totalMaxThreads}
              suffix="个"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="活跃配置"
              value={configs.length}
              suffix="个"
            />
          </Card>
        </Col>
      </Row>

      <Card 
        className="config-card"
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>线程池配置列表</span>
            <Space>
              <Button 
                icon={<ReloadOutlined />} 
                onClick={loadConfigs}
                loading={loading}
              >
                刷新
              </Button>
              <Button 
                type="primary" 
                icon={<PlusOutlined />}
                onClick={() => navigate('/config/add')}
              >
                新增配置
              </Button>
            </Space>
          </div>
        }
      >
        <Table
          columns={columns}
          dataSource={configs}
          rowKey="threadPoolName"
          loading={loading}
          scroll={{ x: 1000 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条配置`,
          }}
        />
      </Card>
    </div>
  )
}

export default ThreadPoolConfigList