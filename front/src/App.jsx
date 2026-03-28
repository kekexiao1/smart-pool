import React from 'react'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { Layout } from 'antd'
import Header from './components/Header'
import ThreadPoolConfigList from './pages/ThreadPoolConfigList'
import ThreadPoolConfigForm from './pages/ThreadPoolConfigForm'
import LogQuery from './pages/LogQuery'

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
          </Routes>
        </Content>
      </Layout>
    </Router>
  )
}

export default App