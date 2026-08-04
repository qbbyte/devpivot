import request from '@/utils/request'

// 查询AI模型调用日志列表
export function listLog(query) {
  return request({
    url: '/system/log/list',
    method: 'get',
    params: query
  })
}

// 查询AI模型调用日志详细
export function getLog(logId) {
  return request({
    url: '/system/log/' + logId,
    method: 'get'
  })
}

// 新增AI模型调用日志
export function addLog(data) {
  return request({
    url: '/system/log',
    method: 'post',
    data: data
  })
}

// 修改AI模型调用日志
export function updateLog(data) {
  return request({
    url: '/system/log',
    method: 'put',
    data: data
  })
}

// 删除AI模型调用日志
export function delLog(logId) {
  return request({
    url: '/system/log/' + logId,
    method: 'delete'
  })
}
