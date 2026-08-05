import request from '@/utils/request'

// 获取可用模型列表
export function getModels() {
  return request({
    url: '/api/ai/models',
    method: 'get'
  })
}

// 获取系统配置（最大对比模型数）
export function getModelConfig() {
  return request({
    url: '/api/ai/models/config',
    method: 'get'
  })
}

// 获取澄清会话
export function getClarifySession(projectId) {
  return request({
    url: `/api/ai/clarify/session/${projectId}`,
    method: 'get'
  })
}

// 发送消息
export function sendMessage(data) {
  return request({
    url: '/api/ai/clarify/send',
    method: 'post',
    data
  })
}

// 获取澄清进度
export function getClarifyProgress(projectId) {
  return request({
    url: `/api/ai/clarify/progress/${projectId}`,
    method: 'get'
  })
}

// 提交澄清结果
export function submitClarify(projectId) {
  return request({
    url: `/api/ai/clarify/submit/${projectId}`,
    method: 'post',
    params: { projectId }
  })
}

// 采纳模型回答
export function adoptAnswer(data) {
  return request({
    url: '/api/ai/clarify/adopt',
    method: 'post',
    data
  })
}
