import request from '@/utils/request'

// 查询AI模型配置列表
export function listAiconfig(query) {
  return request({
    url: '/system/aiconfig/list',
    method: 'get',
    params: query
  })
}

// 查询AI模型配置详细
export function getAiconfig(modelId) {
  return request({
    url: '/system/aiconfig/' + modelId,
    method: 'get'
  })
}

// 新增AI模型配置
export function addAiconfig(data) {
  return request({
    url: '/system/aiconfig',
    method: 'post',
    data: data
  })
}

// 修改AI模型配置
export function updateAiconfig(data) {
  return request({
    url: '/system/aiconfig',
    method: 'put',
    data: data
  })
}

// 删除AI模型配置
export function delAiconfig(modelId) {
  return request({
    url: '/system/aiconfig/' + modelId,
    method: 'delete'
  })
}

// 测试AI模型配置是否可用（单独放大超时，避免模型响应慢被 10s 默认超时截断）
export function testAiconfig(modelId) {
  return request({
    url: '/system/aiconfig/test/' + modelId,
    method: 'get',
    timeout: 60000
  })
}
