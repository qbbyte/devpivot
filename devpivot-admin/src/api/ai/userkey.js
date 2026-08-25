import request from '@/utils/request'

// 我的 API Key 列表（脱敏展示）
export function listMyKeys() {
  return request({ url: '/portal/userkey/my', method: 'get' })
}

// 新增我的 Key
export function addMyKey(data) {
  return request({ url: '/portal/userkey', method: 'post', data })
}

// 修改我的 Key（apiKey 留空表示保留原值）
export function updateMyKey(data) {
  return request({ url: '/portal/userkey', method: 'put', data })
}

// 删除我的 Key
export function delMyKey(keyId) {
  return request({ url: '/portal/userkey/' + keyId, method: 'delete' })
}
