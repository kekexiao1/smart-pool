import axios from 'axios'

const API_BASE_URL = '/admin'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
})

export const threadPoolConfigAPI = {
  getConfigList: () => api.get('/config/list'),
  
  addConfig: (configData) => api.post('/config/add', configData),
  
  updateConfig: (configData) => api.put('/config/update', configData),
  
  deleteConfig: (threadPoolName) => api.delete(`/config/delete/${threadPoolName}`),
  
  getTemplates: () => api.get('/config/templates'),
  
  createFromTemplate: (templateName, applicationName, environment, threadPoolName) => 
    api.post(`/config/create-from-template?templateName=${templateName}&applicationName=${applicationName}&environment=${environment}&threadPoolName=${threadPoolName}`),
  
  getConfigDetail: (threadPoolName) => api.get(`/config/detail/${threadPoolName}`),
}

export const logQueryAPI = {
  queryConfigLogs: (params) => api.get('/log/config', { params }),
  
  queryAlertLogs: (params) => api.get('/log/alert', { params }),
  
  handleAlert: (id, handler) => api.put(`/log/alert/${id}/handle`, null, { params: { handler } }),
}

export const threadPoolMetricsAPI = {
  // 获取所有线程池的最新指标
  getAllLatestMetrics: () => api.get('/metrics/latest'),
  
  // 获取指定线程池的最新指标
  getLatestMetrics: (threadPoolName) => api.get(`/metrics/${threadPoolName}/latest`),
  
  // 获取指定线程池的历史指标
  getMetricsHistory: (threadPoolName, limit = 50) => api.get(`/metrics/${threadPoolName}/history`, { params: { limit } }),
  
  // 获取指定线程池的时间序列数据
  getTimeSeriesData: (threadPoolName, startTime, endTime) => api.get(`/metrics/${threadPoolName}/timeseries`, { 
    params: { startTime, endTime } 
  }),
  
  // 获取最近一小时的时间序列数据
  getRecentTimeSeriesData: (threadPoolName) => api.get(`/metrics/${threadPoolName}/timeseries/recent`),
  
  // 获取拒绝任务趋势数据
  getRejectTrend: (threadPoolName, minutesAgo = 5) => api.get(`/metrics/${threadPoolName}/reject-trend`, { 
    params: { minutesAgo } 
  }),
}

export default api