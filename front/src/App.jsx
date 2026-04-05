import React from 'react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { Layout } from 'antd'
import Header from './components/Header'
import ThreadPoolConfigList from './pages/ThreadPoolConfigList'
import ThreadPoolConfigForm from './pages/ThreadPoolConfigForm'
import LogQuery from './pages/LogQuery'
import ThreadPoolMonitor from './pages/ThreadPoolMonitor'
import ThreadPoolMonitorDetail from './pages/ThreadPoolMonitorDetail'

const { Content } = Layout

function App() {
  return (
    <Router>
      <Layout style={{ minHeight: '100vh' }}>
        <Header />
        <Content className="layout-content">
          <Routes>
            <Route path="/" element={<ThreadPoolConfigList />} />
            <Route path="/config/add" element={<ThreadPoolConfigForm />} />
            <Route path="/config/edit/:threadPoolName" element={<ThreadPoolConfigForm />} />
            <Route path="/log" element={<LogQuery />} />
            <Route path="/monitor" element={<ThreadPoolMonitor />} />
            <Route path="/monitor/detail/:threadPoolName" element={<ThreadPoolMonitorDetail />} />
          </Routes>
        </Content>
      </Layout>
    </Router>
  )
}

export default App