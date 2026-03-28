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

export default api