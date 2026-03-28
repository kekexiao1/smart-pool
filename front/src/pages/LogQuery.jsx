import React, { useState, useEffect } from 'react'
import { 
  Table, 
  Card, 
  Button, 
  Space, 
  Tag, 
  message, 
  Tabs,
  Form,
  Input,
  DatePicker,
  Select,
  Modal
} from 'antd'
import { 
  ReloadOutlined, 
  SearchOutlined,
  CheckOutlined,
  FileTextOutlined,
  AlertOutlined
} from '@ant-design/icons'
import { logQueryAPI } from '../services/api'

const { RangePicker } = DatePicker

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function LogQuery() {
  const [configLogs, setConfigLogs] = useState([])
  const [alertLogs, setAlertLogs] = useState([])
  const [configTotal, setConfigTotal] = useState(0)
  const [alertTotal, setAlertTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [activeTab, setActiveTab] = useState('config')
  const [configForm] = Form.useForm()
  const [alertForm] = Form.useForm()
  const [handleModalVisible, setHandleModalVisible] = useState(false)
  const [currentAlert, setCurrentAlert] = useState(null)
  const [handlerForm] = Form.useForm()
  const [configPagination, setConfigPagination] = useState({ pageNum: 1, pageSize: 10 })
  const [alertPagination, setAlertPagination] = useState({ pageNum: 1, pageSize: 10 })

  const loadConfigLogs = async (params = {}) => {
    setLoading(true)
    try {
      const response = await logQueryAPI.queryConfigLogs(params)
      if (response.data.code === 200) {
        setConfigLogs(response.data.data?.list || [])
        setConfigTotal(response.data.data?.total || 0)
      } else {
        message.error('获取配置日志失败：' + response.data.msg)
      }
    } catch (error) {
      message.error('获取配置日志失败：' + error.message)
    } finally {
      setLoading(false)
    }
  }

  const loadAlertLogs = async (params = {}) => {
    setLoading(true)
    try {
      const response = await logQueryAPI.queryAlertLogs(params)
      if (response.data.code === 200) {
        setAlertLogs(response.data.data?.list || [])
        setAlertTotal(response.data.data?.total || 0)
      } else {
        message.error('获取告警日志失败：' + response.data.msg)
      }
    } catch (error) {
      message.error('获取告警日志失败：' + error.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadConfigLogs({ pageNum: 1, pageSize: 10 })
  }, [])

  const handleConfigSearch = (values) => {
    const params = {
      poolName: values.poolName,
      changeType: values.changeType,
      operator: values.operator,
      startTime: values.timeRange?.[0]?.format('YYYY-MM-DD HH:mm:ss'),
      endTime: values.timeRange?.[1]?.format('YYYY-MM-DD HH:mm:ss'),
      pageNum: 1,
      pageSize: configPagination.pageSize,
    }
    Object.keys(params).forEach(key => {
      if (params[key] === undefined || params[key] === null || params[key] === '') {
        delete params[key]
      }
    })
    setConfigPagination({ pageNum: 1, pageSize: configPagination.pageSize })
    loadConfigLogs(params)
  }

  const handleAlertSearch = (values) => {
    const params = {
      poolName: values.poolName,
      alertType: values.alertType,
      status: values.status,
      startTime: values.timeRange?.[0]?.format('YYYY-MM-DD HH:mm:ss'),
      endTime: values.timeRange?.[1]?.format('YYYY-MM-DD HH:mm:ss'),
      pageNum: 1,
      pageSize: alertPagination.pageSize,
    }
    Object.keys(params).forEach(key => {
      if (params[key] === undefined || params[key] === null || params[key] === '') {
        delete params[key]
      }
    })
    setAlertPagination({ pageNum: 1, pageSize: alertPagination.pageSize })
    loadAlertLogs(params)
  }

  const handleConfigTableChange = (pagination) => {
    const params = {
      ...configForm.getFieldsValue(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (params.timeRange) {
      params.startTime = params.timeRange[0]?.format('YYYY-MM-DD HH:mm:ss')
      params.endTime = params.timeRange[1]?.format('YYYY-MM-DD HH:mm:ss')
      delete params.timeRange
    }
    Object.keys(params).forEach(key => {
      if (params[key] === undefined || params[key] === null || params[key] === '') {
        delete params[key]
      }
    })
    setConfigPagination({ pageNum: pagination.current, pageSize: pagination.pageSize })
    loadConfigLogs(params)
  }

  const handleAlertTableChange = (pagination) => {
    const params = {
      ...alertForm.getFieldsValue(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (params.timeRange) {
      params.startTime = params.timeRange[0]?.format('YYYY-MM-DD HH:mm:ss')
      params.endTime = params.timeRange[1]?.format('YYYY-MM-DD HH:mm:ss')
      delete params.timeRange
    }
    Object.keys(params).forEach(key => {
      if (params[key] === undefined || params[key] === null || params[key] === '') {
        delete params[key]
      }
    })
    setAlertPagination({ pageNum: pagination.current, pageSize: pagination.pageSize })
    loadAlertLogs(params)
  }

  const handleAlert = (record) => {
    setCurrentAlert(record)
    setHandleModalVisible(true)
  }

  const confirmHandleAlert = async (values) => {
    try {
      const response = await logQueryAPI.handleAlert(currentAlert.id, values.handler)
      if (response.data.code === 200) {
        message.success('处理告警成功')
        setHandleModalVisible(false)
        handlerForm.resetFields()
        loadAlertLogs({ ...alertPagination, ...alertForm.getFieldsValue() })
      } else {
        message.error('处理告警失败：' + response.data.msg)
      }
    } catch (error) {
      message.error('处理告警失败：' + error.message)
    }
  }

  const configColumns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '线程池名称',
      dataIndex: 'poolName',
      key: 'poolName',
      width: 180,
      render: (text) => <Tag color="blue">{text}</Tag>,
    },
    {
      title: '操作类型',
      dataIndex: 'changeType',
      key: 'changeType',
      width: 100,
      render: (text) => {
        const colorMap = { INIT: 'green', UPDATE: 'orange', CREATE: 'green', DELETE: 'red' }
        return <Tag color={colorMap[text] || 'default'}>{text}</Tag>
      },
    },
    {
      title: '变更前配置',
      dataIndex: 'oldConfig',
      key: 'oldConfig',
      ellipsis: true,
      render: (text) => text || '-',
    },
    {
      title: '变更后配置',
      dataIndex: 'newConfig',
      key: 'newConfig',
      ellipsis: true,
      render: (text) => text || '-',
    },
    {
      title: '操作人',
      dataIndex: 'operator',
      key: 'operator',
      width: 120,
      render: (text) => text || '-',
    },
    {
      title: '操作时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (text) => formatDateTime(text),
    },
  ]

  const alertColumns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '线程池名称',
      dataIndex: 'poolName',
      key: 'poolName',
      width: 180,
      render: (text) => <Tag color="blue">{text}</Tag>,
    },
    {
      title: '告警类型',
      dataIndex: 'alertType',
      key: 'alertType',
      width: 120,
      render: (text) => <Tag color="red">{text}</Tag>,
    },
    {
      title: '告警内容',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => (
        <Tag color={status === 1 ? 'green' : 'orange'}>
          {status === 1 ? '已处理' : '未处理'}
        </Tag>
      ),
    },
    {
      title: '处理人',
      dataIndex: 'handler',
      key: 'handler',
      width: 120,
      render: (text) => text || '-',
    },
    {
      title: '告警时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (text) => formatDateTime(text),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        record.status === 1 ? (
          <Tag color="green">已处理</Tag>
        ) : (
          <Button 
            type="link" 
            icon={<CheckOutlined />} 
            onClick={() => handleAlert(record)}
          >
            处理
          </Button>
        )
      ),
    },
  ]

  const tabItems = [
    {
      key: 'config',
      label: (
        <span>
          <FileTextOutlined />
          配置变更日志
        </span>
      ),
      children: (
        <div>
          <Form
            form={configForm}
            layout="inline"
            onFinish={handleConfigSearch}
            style={{ marginBottom: 16 }}
          >
            <Form.Item name="poolName" style={{ marginBottom: 8 }}>
              <Input placeholder="线程池名称" allowClear style={{ width: 150 }} />
            </Form.Item>
            <Form.Item name="operator" style={{ marginBottom: 8 }}>
              <Input placeholder="操作人" allowClear style={{ width: 120 }} />
            </Form.Item>
            <Form.Item name="changeType" style={{ marginBottom: 8 }}>
              <Select placeholder="操作类型" allowClear style={{ width: 120 }}>
                <Select.Option value="INIT">初始化</Select.Option>
                <Select.Option value="UPDATE">更新</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item name="timeRange" style={{ marginBottom: 8 }}>
              <RangePicker 
                showTime={{ format: 'HH:mm:ss' }}
                format="YYYY-MM-DD HH:mm:ss"
                placeholder={['开始时间', '结束时间']}
              />
            </Form.Item>
            <Form.Item style={{ marginBottom: 8 }}>
              <Space>
                <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                  查询
                </Button>
                <Button onClick={() => { configForm.resetFields(); setConfigPagination({ pageNum: 1, pageSize: 10 }); loadConfigLogs({ pageNum: 1, pageSize: 10 }); }}>
                  重置
                </Button>
              </Space>
            </Form.Item>
          </Form>
          <Table
            columns={configColumns}
            dataSource={configLogs}
            rowKey="id"
            loading={loading}
            scroll={{ x: 1200 }}
            pagination={{
              current: configPagination.pageNum,
              pageSize: configPagination.pageSize,
              total: configTotal,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 条记录`,
            }}
            onChange={handleConfigTableChange}
          />
        </div>
      ),
    },
    {
      key: 'alert',
      label: (
        <span>
          <AlertOutlined />
          告警日志
        </span>
      ),
      children: (
        <div>
          <Form
            form={alertForm}
            layout="inline"
            onFinish={handleAlertSearch}
            style={{ marginBottom: 16 }}
          >
            <Form.Item name="poolName" style={{ marginBottom: 8 }}>
              <Input placeholder="线程池名称" allowClear style={{ width: 150 }} />
            </Form.Item>
            <Form.Item name="alertType" style={{ marginBottom: 8 }}>
              <Input placeholder="告警类型" allowClear style={{ width: 120 }} />
            </Form.Item>
            <Form.Item name="status" style={{ marginBottom: 8 }}>
              <Select placeholder="状态" allowClear style={{ width: 120 }}>
                <Select.Option value={0}>未处理</Select.Option>
                <Select.Option value={1}>已处理</Select.Option>
              </Select>
            </Form.Item>
            <Form.Item name="timeRange" style={{ marginBottom: 8 }}>
              <RangePicker 
                showTime={{ format: 'HH:mm:ss' }}
                format="YYYY-MM-DD HH:mm:ss"
                placeholder={['开始时间', '结束时间']}
              />
            </Form.Item>
            <Form.Item style={{ marginBottom: 8 }}>
              <Space>
                <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
                  查询
                </Button>
                <Button onClick={() => { alertForm.resetFields(); setAlertPagination({ pageNum: 1, pageSize: 10 }); loadAlertLogs({ pageNum: 1, pageSize: 10 }); }}>
                  重置
                </Button>
              </Space>
            </Form.Item>
          </Form>
          <Table
            columns={alertColumns}
            dataSource={alertLogs}
            rowKey="id"
            loading={loading}
            scroll={{ x: 1200 }}
            pagination={{
              current: alertPagination.pageNum,
              pageSize: alertPagination.pageSize,
              total: alertTotal,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 条记录`,
            }}
            onChange={handleAlertTableChange}
          />
        </div>
      ),
    },
  ]

  const handleTabChange = (key) => {
    setActiveTab(key)
    if (key === 'config') {
      setConfigPagination({ pageNum: 1, pageSize: 10 })
      loadConfigLogs({ pageNum: 1, pageSize: 10 })
    } else {
      setAlertPagination({ pageNum: 1, pageSize: 10 })
      loadAlertLogs({ pageNum: 1, pageSize: 10 })
    }
  }

  return (
    <div>
      <Card 
        className="config-card"
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>日志查询</span>
            <Button 
              icon={<ReloadOutlined />} 
              onClick={() => activeTab === 'config' ? loadConfigLogs(configPagination) : loadAlertLogs(alertPagination)}
              loading={loading}
            >
              刷新
            </Button>
          </div>
        }
      >
        <Tabs 
          activeKey={activeTab} 
          items={tabItems}
          onChange={handleTabChange}
        />
      </Card>

      <Modal
        title="处理告警"
        open={handleModalVisible}
        onCancel={() => { setHandleModalVisible(false); handlerForm.resetFields(); }}
        onOk={() => handlerForm.submit()}
      >
        <Form
          form={handlerForm}
          onFinish={confirmHandleAlert}
          layout="vertical"
        >
          <Form.Item
            name="handler"
            label="处理人"
            rules={[{ required: true, message: '请输入处理人' }]}
          >
            <Input placeholder="请输入处理人姓名" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default LogQuery
