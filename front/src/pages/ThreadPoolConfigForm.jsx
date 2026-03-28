import React, { useState, useEffect } from 'react'
import { 
  Form, 
  Input, 
  InputNumber, 
  Select, 
  Card, 
  Button, 
  Space, 
  message, 
  Row, 
  Col,
  Divider,
  Tag,
  Alert
} from 'antd'
import { 
  SaveOutlined, 
  ArrowLeftOutlined, 
  ReloadOutlined,
  RocketOutlined 
} from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import { threadPoolConfigAPI } from '../services/api'

const { Option } = Select

function ThreadPoolConfigForm() {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [templates, setTemplates] = useState([])
  const [selectedTemplate, setSelectedTemplate] = useState(null)
  const navigate = useNavigate()
  const { threadPoolName } = useParams()
  const isEditMode = !!threadPoolName

  const rejectedHandlers = [
    { label: 'CallerRunsPolicy', value: 'CallerRunsPolicy' },
    { label: 'AbortPolicy', value: 'AbortPolicy' },
    { label: 'DiscardPolicy', value: 'DiscardPolicy' },
    { label: 'DiscardOldestPolicy', value: 'DiscardOldestPolicy' },
  ]

  const timeUnits = [
    { label: '秒', value: 'SECONDS' },
    { label: '毫秒', value: 'MILLISECONDS' },
    { label: '分钟', value: 'MINUTES' },
    { label: '小时', value: 'HOURS' },
  ]

  const loadTemplates = async () => {
    try {
      const response = await threadPoolConfigAPI.getTemplates()
      if (response.data.code === 200) {
        setTemplates(response.data.data || [])
      }
    } catch (error) {
      console.error('加载模板失败:', error)
    }
  }

  const loadConfigDetail = async () => {
    if (!isEditMode) return
    
    setLoading(true)
    try {
      const response = await threadPoolConfigAPI.getConfigDetail(threadPoolName)
      if (response.data.code === 200 && response.data.data) {
        const config = response.data.data
        form.setFieldsValue({
          applicationName: config.applicationName || '',
          environment: config.environment || 'default',
          threadPoolName: config.threadPoolName,
          corePoolSize: config.config?.corePoolSize,
          maximumPoolSize: config.config?.maximumPoolSize,
          keepAliveTime: config.config?.keepAliveTime,
          unit: config.config?.unit || 'SECONDS',
          queueCapacity: config.config?.queueCapacity,
          rejectedHandler: config.config?.rejectedHandlerClass || 'CallerRunsPolicy',
        })
      } else {
        message.error('获取配置详情失败')
        navigate('/')
      }
    } catch (error) {
      message.error('获取配置详情失败：' + error.message)
      navigate('/')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadTemplates()
    if (isEditMode) {
      loadConfigDetail()
    }
  }, [isEditMode, threadPoolName])

  const handleTemplateChange = (templateName) => {
    const template = templates.find(t => t.templateName === templateName)
    if (template) {
      setSelectedTemplate(template)
      form.setFieldsValue({
        corePoolSize: template.corePoolSize,
        maximumPoolSize: template.maximumPoolSize,
        keepAliveTime: template.keepAliveTime,
        queueCapacity: template.queueCapacity,
        rejectedHandler: template.rejectedHandler,
      })
      message.info(`已应用模板: ${template.templateName}`)
    }
  }

  const handleThreadPoolNameChange = (e) => {
    const threadPoolName = e.target.value
    if (threadPoolName && !isEditMode) {
      // 自动设置应用名称为线程池名称
      const currentAppName = form.getFieldValue('applicationName')
      if (!currentAppName || currentAppName === '') {
        form.setFieldsValue({
          applicationName: threadPoolName
        })
      }
    }
  }

  const handleSubmit = async (values) => {
    setLoading(true)
    try {
      const submitData = {
        ...values,
        // 确保数值类型正确
        corePoolSize: Number(values.corePoolSize),
        maximumPoolSize: Number(values.maximumPoolSize),
        keepAliveTime: Number(values.keepAliveTime),
        queueCapacity: Number(values.queueCapacity),
      }

      // 如果是更新模式且应用名称为空，则从表单中移除该字段，让后端保持原值
      if (isEditMode && (!values.applicationName || values.applicationName.trim() === '')) {
        delete submitData.applicationName
      }

      let response
      if (isEditMode) {
        response = await threadPoolConfigAPI.updateConfig(submitData)
      } else {
        response = await threadPoolConfigAPI.addConfig(submitData)
      }

      if (response.data.code === 200) {
        message.success(isEditMode ? '更新配置成功' : '新增配置成功')
        navigate('/')
      } else {
        message.error((isEditMode ? '更新' : '新增') + '配置失败：' + response.data.msg)
      }
    } catch (error) {
      message.error((isEditMode ? '更新' : '新增') + '配置失败：' + error.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <Card 
        className="config-card"
        title={
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>
              {isEditMode ? '编辑线程池配置' : '新增线程池配置'}
              {isEditMode && <Tag color="blue" style={{ marginLeft: 8 }}>{threadPoolName}</Tag>}
            </span>
            <Space>
              <Button 
                icon={<ArrowLeftOutlined />}
                onClick={() => navigate('/')}
              >
                返回列表
              </Button>
            </Space>
          </div>
        }
      >
        {templates.length > 0 && !isEditMode && (
          <>
            <Alert
              message="快速配置"
              description="选择预设模板可以快速创建配置，您也可以手动调整各项参数"
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />
            <Row gutter={16} style={{ marginBottom: 24 }}>
              <Col span={24}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ fontWeight: 'bold' }}>选择模板:</span>
                  <Select
                    placeholder="选择配置模板"
                    style={{ width: 200 }}
                    onChange={handleTemplateChange}
                    allowClear
                  >
                    {templates.map(template => (
                      <Option key={template.templateName} value={template.templateName}>
                        {template.templateName} - {template.description}
                      </Option>
                    ))}
                  </Select>
                  {selectedTemplate && (
                    <Tag color="green" icon={<RocketOutlined />}>
                      已选择: {selectedTemplate.templateName}
                    </Tag>
                  )}
                </div>
              </Col>
            </Row>
            <Divider />
          </>
        )}

        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
          environment: 'default',
          rejectedHandler: 'CallerRunsPolicy',
          unit: 'SECONDS',
        }}
        >
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item
                label="应用名称"
                name="applicationName"
                rules={[{ required: !isEditMode, message: '请输入应用名称' }]}
                help={isEditMode ? "更新配置时应用名称可选，留空将保持原值" : "输入线程池名称后会自动填充应用名称"}
              >
                <Input placeholder="例如: order-service" />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="环境"
                name="environment"
                rules={[{ required: true, message: '请选择环境' }]}
              >
                <Select placeholder="选择环境">
                  <Option value="dev">开发环境</Option>
                  <Option value="test">测试环境</Option>
                  <Option value="prod">生产环境</Option>
                  <Option value="default">默认</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item
                label="线程池名称"
                name="threadPoolName"
                rules={[{ required: true, message: '请输入线程池名称' }]}
              >
                <Input 
                  placeholder="例如: order-process-pool" 
                  disabled={isEditMode}
                  onChange={handleThreadPoolNameChange}
                />
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left">核心参数</Divider>
          
          <Row gutter={16}>
            <Col span={6}>
              <Form.Item
                label="核心线程数"
                name="corePoolSize"
                rules={[{ required: true, message: '请输入核心线程数' }]}
              >
                <InputNumber 
                  min={1} 
                  max={1000} 
                  style={{ width: '100%' }} 
                  placeholder="例如: 8"
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item
                label="最大线程数"
                name="maximumPoolSize"
                rules={[{ required: true, message: '请输入最大线程数' }]}
              >
                <InputNumber 
                  min={1} 
                  max={10000} 
                  style={{ width: '100%' }} 
                  placeholder="例如: 16"
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item
                label="存活时间"
                name="keepAliveTime"
                rules={[{ required: true, message: '请输入存活时间' }]}
              >
                <InputNumber 
                  min={1} 
                  max={3600} 
                  style={{ width: '100%' }} 
                  placeholder="例如: 60"
                />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item
                label="时间单位"
                name="unit"
                rules={[{ required: true, message: '请选择时间单位' }]}
              >
                <Select placeholder="选择时间单位">
                  {timeUnits.map(unit => (
                    <Option key={unit.value} value={unit.value}>
                      {unit.label}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Divider orientation="left">队列配置</Divider>
          
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="队列容量"
                name="queueCapacity"
                rules={[{ required: true, message: '请输入队列容量' }]}
              >
                <InputNumber 
                  min={1} 
                  max={100000} 
                  style={{ width: '100%' }} 
                  placeholder="例如: 512"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                label="拒绝策略"
                name="rejectedHandler"
                rules={[{ required: true, message: '请选择拒绝策略' }]}
              >
                <Select placeholder="选择拒绝策略">
                  {rejectedHandlers.map(handler => (
                    <Option key={handler.value} value={handler.value}>
                      {handler.label}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>



          <Form.Item style={{ marginTop: 32, textAlign: 'center' }}>
            <Space size="large">
              <Button 
                type="primary" 
                htmlType="submit" 
                icon={<SaveOutlined />}
                loading={loading}
                size="large"
              >
                {isEditMode ? '更新配置' : '创建配置'}
              </Button>
              <Button 
                icon={<ReloadOutlined />}
                onClick={() => form.resetFields()}
                size="large"
              >
                重置
              </Button>
              <Button 
                icon={<ArrowLeftOutlined />}
                onClick={() => navigate('/')}
                size="large"
              >
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default ThreadPoolConfigForm