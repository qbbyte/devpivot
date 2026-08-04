import request from '@/utils/request'

// 查询用户API Key配置列表
export function listKey(query) {
  return request({
    url: '/system/key/list',
    method: 'get',
    params: query
  })
}

// 查询用户API Key配置详细
export function getKey(keyId) {
  return request({
    url: '/system/key/' + keyId,
    method: 'get'
  })
}

// 新增用户API Key配置
export function addKey(data) {
  return request({
    url: '/system/key',
    method: 'post',
    data: data
  })
}

// 修改用户API Key配置
export function updateKey(data) {
  return request({
    url: '/system/key',
    method: 'put',
    data: data
  })
}

// 删除用户API Key配置
export function delKey(keyId) {
  return request({
    url: '/system/key/' + keyId,
    method: 'delete'
  })
}
