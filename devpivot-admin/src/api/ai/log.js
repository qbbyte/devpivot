import request from '@/utils/request'

// 查询AI模型调用日志列表
export function listLog(query) {
  return request({
    url: '/system/ailog/list',
    method: 'get',
    params: query
  })
}

// 查询AI模型调用日志详细
export function getLog(logId) {
  return request({
    url: '/system/ailog/' + logId,
    method: 'get'
  })
}

// 新增AI模型调用日志
export function addLog(data) {
  return request({
    url: '/system/ailog',
    method: 'post',
    data: data
  })
}

// 修改AI模型调用日志
export function updateLog(data) {
  return request({
    url: '/system/ailog',
    method: 'put',
    data: data
  })
}

// 删除AI模型调用日志
export function delLog(logId) {
  return request({
    url: '/system/ailog/' + logId,
    method: 'delete'
  })
}
