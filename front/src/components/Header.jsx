import React from 'react'
import { Layout, Menu, Button, Space } from 'antd'
import { PlusOutlined, SettingOutlined, DashboardOutlined, BarChartOutlined, FileTextOutlined } from '@ant-design/icons'
import { useNavigate, useLocation } from 'react-router-dom'

const { Header: AntHeader } = Layout

function Header() {
  const navigate = useNavigate()
  const location = useLocation()

  const menuItems = [
    {
      key: '/',
      icon: <DashboardOutlined />,
      label: '配置管理',
    },
    {
      key: '/monitor',
      icon: <BarChartOutlined />,
      label: '实时监控',
    },
    {
      key: '/log',
      icon: <FileTextOutlined />,
      label: '日志查询',
    },
  ]

  return (
    <AntHeader className="layout-header">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <h1 style={{ color: 'white', margin: 0, marginRight: '40px' }}>
            动态线程池管理平台
          </h1>
          <Menu
            theme="dark"
            mode="horizontal"
            selectedKeys={[location.pathname]}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
            style={{ border: 'none', background: 'transparent', minWidth: 400 }}
            overflowedIndicator={null}
          />
        </div>
        
        <Space>
          <Button 
            type="primary" 
            icon={<BarChartOutlined />}
            onClick={() => window.open('http://localhost:3000/goto/tr57brcvR?orgId=1', '_blank')}
          >
            Grafana监控
          </Button>
        </Space>
      </div>
    </AntHeader>
  )
}

export default Header